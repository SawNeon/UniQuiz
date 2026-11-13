import { criarSessaoVirtual } from "./cliente.mjs";
import { PIN_DOS_USUARIOS, PREFIXO_DOS_DADOS, respostaCertaPara } from "./dados.mjs";
import { emParalelo, pausar, pensar } from "./tempo.mjs";

async function medir(metricas, rotulo, sessao, pedido) {
  const resultado = await sessao.requisitar(pedido);
  metricas.registrar(rotulo, resultado);
  return resultado;
}

function entrarComo(colaborador) {
  return {
    metodo: "POST",
    caminho: "/api/sessao",
    corpo: { colaboradorId: colaborador.id, pin: PIN_DOS_USUARIOS },
  };
}

async function jornadaDaLicao({ cliente, colaborador, dados, metricas, pensarMs, totalEsperado }) {
  const sessao = criarSessaoVirtual(cliente);
  const login = await medir(metricas, "entrar (PIN)", sessao, entrarComo(colaborador));
  if (login.status !== 204) {
    return;
  }
  await medir(metricas, "perfil", sessao, { caminho: "/api/eu" });
  await medir(metricas, "caminho", sessao, { caminho: "/api/caminho" });

  const inicio = await medir(metricas, "iniciar lição", sessao, {
    metodo: "POST",
    caminho: `/api/licoes/${dados.licaoId}/tentativas`,
  });
  if (inicio.status !== 201) {
    return;
  }
  conferir(metricas, inicio.corpo.desafios.length === totalEsperado, "lição sorteou a quantidade pedida");
  await responderTodos({ sessao, tentativa: inicio.corpo, metricas, pensarMs });

  const fim = await medir(metricas, "concluir lição", sessao, {
    metodo: "POST",
    caminho: `/api/tentativas/${inicio.corpo.tentativaId}/concluir`,
  });
  if (fim.status === 200) {
    conferir(metricas, fim.corpo.perfeita === true && fim.corpo.acertos === totalEsperado, "lição concluída perfeita");
  }
  await medir(metricas, "classificação", sessao, { caminho: "/api/classificacao" });
}

async function responderTodos({ sessao, tentativa, metricas, pensarMs }) {
  for (const desafio of tentativa.desafios) {
    await pensar(pensarMs);
    const correcao = await medir(metricas, "responder desafio", sessao, {
      metodo: "POST",
      caminho: `/api/tentativas/${tentativa.tentativaId}/respostas`,
      corpo: { desafioId: desafio.id, resposta: respostaCertaPara(desafio) },
    });
    if (correcao.status === 200) {
      conferir(metricas, correcao.corpo.correta === true, "resposta certa corrigida como certa");
    }
  }
}

function conferir(metricas, condicao, descricao) {
  if (!condicao) {
    metricas.registrarFalhaDeVerificacao(`verificação falhou: ${descricao}`);
  }
}

export async function cenarioLeitura({ cliente, metricas, concorrencia, duracaoMs }) {
  const sessao = criarSessaoVirtual(cliente);
  const fim = performance.now() + duracaoMs;
  const trabalhadores = Array.from({ length: concorrencia }, async () => {
    while (performance.now() < fim) {
      const empresas = await medir(metricas, "lista de empresas", sessao, { caminho: "/api/empresas" });
      const empresaId = empresas.corpo?.[0]?.id;
      if (empresaId === undefined) {
        continue;
      }
      const setores = await medir(metricas, "lista de setores", sessao, {
        caminho: `/api/empresas/${empresaId}/setores`,
      });
      const setorId = setores.corpo?.[0]?.id;
      if (setorId !== undefined) {
        await medir(metricas, "busca de nomes do setor", sessao, {
          caminho: `/api/setores/${setorId}/colaboradores?busca=${encodeURIComponent(PREFIXO_DOS_DADOS)}`,
        });
      }
    }
  });
  await Promise.all(trabalhadores);
}

export async function cenarioLoginEmRajada({ cliente, dados, metricas, usuarios }) {
  const escolhidos = dados.colaboradores.slice(0, usuarios);
  await Promise.all(
    escolhidos.map((colaborador) =>
      medir(metricas, "entrar (PIN)", criarSessaoVirtual(cliente), entrarComo(colaborador)),
    ),
  );
}

export async function cenarioLicaoCompleta({ cliente, dados, metricas, usuarios, rampaMs, pensarMs, totalEsperado }) {
  const escolhidos = dados.colaboradores.slice(0, usuarios);
  await Promise.all(
    escolhidos.map(async (colaborador, indice) => {
      await pausar((indice / escolhidos.length) * rampaMs);
      await jornadaDaLicao({ cliente, colaborador, dados, metricas, pensarMs, totalEsperado });
    }),
  );
}

export async function cenarioContinuo({ cliente, dados, metricas, usuarios, duracaoMs, pensarMs, totalEsperado }) {
  const escolhidos = dados.colaboradores.slice(0, usuarios);
  const fim = performance.now() + duracaoMs;
  await emParalelo(escolhidos, escolhidos.length, async (colaborador) => {
    while (performance.now() < fim) {
      await jornadaDaLicao({ cliente, colaborador, dados, metricas, pensarMs, totalEsperado });
    }
  });
}

export async function cenarioLimiteDeRequisicoes({ cliente, metricas, requisicoes, concorrencia }) {
  const sessao = criarSessaoVirtual(cliente);
  const numeros = Array.from({ length: requisicoes }, (_, indice) => indice);
  await emParalelo(numeros, concorrencia, () =>
    medir(metricas, "GET /api/empresas", sessao, { caminho: "/api/empresas" }),
  );
}

function rotuloDaTentativaDePin(resultado) {
  if (resultado.status === 204) {
    return "PIN aceito";
  }
  const detalhe = resultado.corpo?.detail ?? "";
  if (detalhe.includes("incorreto")) {
    return "chute avaliado (PIN incorreto)";
  }
  if (detalhe.includes("bloqueado")) {
    return "recusado por bloqueio";
  }
  return `outra resposta (${resultado.status})`;
}

export async function cenarioForcaBrutaDePin({ cliente, dados, metricas, tentativas, concorrencia }) {
  const vitima = dados.colaboradores.at(-1);
  const chutes = Array.from({ length: tentativas }, (_, indice) => String(indice).padStart(3, "0")).filter(
    (pin) => pin !== PIN_DOS_USUARIOS,
  );
  await emParalelo(chutes, concorrencia, async (pin) => {
    const resultado = await criarSessaoVirtual(cliente).requisitar({
      metodo: "POST",
      caminho: "/api/sessao",
      corpo: { colaboradorId: vitima.id, pin },
    });
    metricas.registrar(rotuloDaTentativaDePin(resultado), resultado);
  });
  const doDono = await criarSessaoVirtual(cliente).requisitar(entrarComo(vitima));
  metricas.registrar(`dono entra com o PIN certo depois: ${rotuloDaTentativaDePin(doDono)}`, doDono);
}

function enderecoFalso() {
  const octeto = () => Math.floor(Math.random() * 254) + 1;
  return `${octeto()}.${octeto()}.${octeto()}.${octeto()}`;
}

export async function cenarioLimiteComEnderecoFalso({ cliente, metricas, requisicoes, concorrencia }) {
  const sessao = criarSessaoVirtual(cliente);
  const numeros = Array.from({ length: requisicoes }, (_, indice) => indice);
  await emParalelo(numeros, concorrencia, () =>
    medir(metricas, "GET /api/empresas com X-Forwarded-For falso", sessao, {
      caminho: "/api/empresas",
      cabecalhos: { "x-forwarded-for": enderecoFalso() },
    }),
  );
}

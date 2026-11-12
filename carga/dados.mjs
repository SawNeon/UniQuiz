import { criarSessaoVirtual } from "./cliente.mjs";
import { emParalelo, pausar } from "./tempo.mjs";

export const PIN_DOS_USUARIOS = "123";
export const PREFIXO_DOS_DADOS = "Carga";

const ESPERA_AO_LIMITAR_MS = 4_000;
const TENTATIVAS_AO_LIMITAR = 30;
const PARALELISMO_DO_PREPARO = 6;

export async function prepararDados({ cliente, administrador, usuarios, banco, sorteio }) {
  const sessaoAdmin = criarSessaoVirtual(cliente);
  const preparo = criarPreparo(sessaoAdmin);
  await preparo.entrarComoAdmin(administrador);

  const moduloId = await preparo.criarModulo(`${PREFIXO_DOS_DADOS} Módulo`);
  const licaoId = await preparo.criarLicao(moduloId, `${PREFIXO_DOS_DADOS} Lição`, sorteio);
  await preparo.publicarBanco(licaoId, banco);
  await preparo.publicar("licoes", licaoId);
  await preparo.publicar("modulos", moduloId);

  const colaboradores = await preparo.cadastrarColaboradores(usuarios);
  return { moduloId, licaoId, colaboradores, limitesAtingidosNoPreparo: preparo.limitesAtingidos() };
}

export async function reutilizarDados({ cliente, administrador }) {
  const sessaoAdmin = criarSessaoVirtual(cliente);
  const preparo = criarPreparo(sessaoAdmin);
  await preparo.entrarComoAdmin(administrador);
  const licaoId = await preparo.licaoDeCarga();
  const colaboradores = await listarColaboradoresDeCarga(cliente);
  return { licaoId, colaboradores, limitesAtingidosNoPreparo: preparo.limitesAtingidos() };
}

async function listarColaboradoresDeCarga(cliente) {
  const empresas = await cliente.requisitar({ caminho: "/api/empresas" });
  const empresa = empresas.corpo.find((item) => item.nome === `${PREFIXO_DOS_DADOS} Empresa`);
  const setores = await cliente.requisitar({ caminho: `/api/empresas/${empresa.id}/setores` });
  const setor = setores.corpo.find((item) => item.nome === `${PREFIXO_DOS_DADOS} Setor`);
  const nomes = await cliente.requisitar({ caminho: `/api/setores/${setor.id}/colaboradores` });
  return nomes.corpo
    .filter((pessoa) => pessoa.nome.startsWith(`${PREFIXO_DOS_DADOS} `))
    .map(({ id, nome }) => ({ id, nome }));
}

function criarPreparo(sessao) {
  let limitesAtingidos = 0;

  async function enviar(pedido, statusEsperado) {
    for (let tentativa = 0; tentativa < TENTATIVAS_AO_LIMITAR; tentativa++) {
      const resultado = await sessao.requisitar(pedido);
      if (resultado.status === 429) {
        limitesAtingidos++;
        await pausar(ESPERA_AO_LIMITAR_MS);
        continue;
      }
      if (resultado.status !== statusEsperado) {
        throw new Error(`${pedido.metodo} ${pedido.caminho} deu ${resultado.status}: ${JSON.stringify(resultado.corpo)}`);
      }
      return resultado.corpo;
    }
    throw new Error(`${pedido.metodo} ${pedido.caminho} continuou limitado após ${TENTATIVAS_AO_LIMITAR} tentativas`);
  }

  async function entrarComoAdmin({ usuario, senha }) {
    await enviar({ metodo: "POST", caminho: "/api/admin/login", corpo: { usuario, senha } }, 204);
  }

  async function licaoDeCarga() {
    const resultado = await sessao.requisitar({ caminho: "/api/admin/modulos" });
    const licoes = resultado.corpo.flatMap((modulo) => modulo.licoes);
    return licoes.find((licao) => licao.titulo === `${PREFIXO_DOS_DADOS} Lição`).id;
  }

  async function criarModulo(titulo) {
    const corpo = await enviar(
      { metodo: "POST", caminho: "/api/admin/modulos", corpo: { titulo, descricao: "Dados de teste de carga", materialEstudo: "" } },
      201,
    );
    return corpo.id;
  }

  async function criarLicao(moduloId, titulo, quantidades) {
    const corpo = await enviar(
      { metodo: "POST", caminho: `/api/admin/modulos/${moduloId}/licoes`, corpo: { titulo, quantidades } },
      201,
    );
    return corpo.id;
  }

  async function publicar(recurso, id) {
    await enviar({ metodo: "POST", caminho: `/api/admin/${recurso}/${id}/status`, corpo: { status: "PUBLICADO" } }, 204);
  }

  async function publicarBanco(licaoId, banco) {
    for (const [nivel, quantidade] of Object.entries(banco)) {
      for (let numero = 1; numero <= quantidade; numero++) {
        const corpo = await enviar(
          {
            metodo: "POST",
            caminho: `/api/admin/licoes/${licaoId}/desafios`,
            corpo: desafioDoNivel(nivel, numero),
          },
          201,
        );
        await publicar("desafios", corpo.id);
      }
    }
  }

  async function cadastrarColaborador(numero) {
    const nome = `${PREFIXO_DOS_DADOS} ${String(numero).padStart(4, "0")}`;
    const corpo = await enviar(
      {
        metodo: "POST",
        caminho: "/api/admin/colaboradores",
        corpo: { nome, empresa: `${PREFIXO_DOS_DADOS} Empresa`, setor: `${PREFIXO_DOS_DADOS} Setor`, pin: PIN_DOS_USUARIOS },
      },
      201,
    );
    return { id: corpo.id, nome };
  }

  async function cadastrarColaboradores(quantidade) {
    const numeros = Array.from({ length: quantidade }, (_, indice) => indice + 1);
    const [primeiro, ...demais] = numeros;
    const colaboradores = [await cadastrarColaborador(primeiro)];
    await emParalelo(demais, PARALELISMO_DO_PREPARO, async (numero) => {
      colaboradores.push(await cadastrarColaborador(numero));
    });
    return colaboradores;
  }

  return {
    entrarComoAdmin,
    licaoDeCarga,
    criarModulo,
    criarLicao,
    publicar,
    publicarBanco,
    cadastrarColaboradores,
    limitesAtingidos: () => limitesAtingidos,
  };
}

export function desafioDoNivel(nivel, numero) {
  const comum = {
    enunciado: `Desafio ${nivel} ${numero}`,
    dificuldade: nivel,
    pontos: 10,
    explicacao: "Explicação do desafio de teste.",
  };
  if (nivel === "MEDIO") {
    return {
      ...comum,
      tipo: "MULTIPLA_ESCOLHA",
      conteudo: {
        alternativas: [
          { id: "a", texto: "Certa", correta: true, motivo: "" },
          { id: "b", texto: "Errada B", correta: false, motivo: "Não é B." },
          { id: "c", texto: "Errada C", correta: false, motivo: "Não é C." },
        ],
      },
    };
  }
  return {
    ...comum,
    tipo: "VERDADEIRO_OU_FALSO",
    conteudo: { afirmacao: `Afirmação ${nivel} ${numero}`, verdadeira: nivel === "FACIL", motivoDoErro: "Motivo do erro." },
  };
}

export function respostaCertaPara(desafio) {
  if (desafio.tipo === "MULTIPLA_ESCOLHA") {
    return { alternativaId: "a" };
  }
  return { valor: desafio.dificuldade === "FACIL" };
}

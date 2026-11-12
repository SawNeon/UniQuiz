import { criarCliente } from "./cliente.mjs";
import {
  cenarioContinuo,
  cenarioForcaBrutaDePin,
  cenarioLeitura,
  cenarioLicaoCompleta,
  cenarioLimiteComEnderecoFalso,
  cenarioLimiteDeRequisicoes,
  cenarioLoginEmRajada,
} from "./cenarios.mjs";
import { prepararDados, reutilizarDados } from "./dados.mjs";
import { criarMetricas, formatarTabela } from "./metricas.mjs";

const OPCOES_PADRAO = {
  base: "http://localhost:8080",
  usuarios: "100",
  cenarios: "leitura,login,licao,continuo",
  banco: "8,8,8",
  sorteio: "5,5,5",
  duracao: "30",
  pensar: "0,0",
  rampa: "0",
  "admin-usuario": "admin",
  "admin-senha-ambiente": "UNIQUIZ_ADMIN_SENHA",
  "tentativas-forca-bruta": "1000",
  "concorrencia-forca-bruta": "200",
  "requisicoes-limite": "500",
  "concorrencia-limite": "20",
};

function lerOpcoes(argumentos) {
  const opcoes = { ...OPCOES_PADRAO };
  for (const argumento of argumentos) {
    const [chave, valor = "true"] = argumento.replace(/^--/, "").split("=");
    opcoes[chave] = valor;
  }
  return opcoes;
}

function numeros(texto) {
  return texto.split(",").map(Number);
}

function paraSorteio([facil, medio, dificil]) {
  return { facil, medio, dificil };
}

function paraBanco([facil, medio, dificil]) {
  return { FACIL: facil, MEDIO: medio, DIFICIL: dificil };
}

async function medirCenario(titulo, cenario) {
  const metricas = criarMetricas();
  const inicio = performance.now();
  await cenario(metricas);
  const duracaoS = ((performance.now() - inicio) / 1000).toFixed(1);
  console.log(formatarTabela(`${titulo} (${duracaoS} s no total)`, metricas.resumo()));
}

async function criarDadosDeTeste(cliente, opcoes) {
  const senha = process.env[opcoes["admin-senha-ambiente"]];
  if (!senha) {
    throw new Error(`Defina a senha do admin na variável ${opcoes["admin-senha-ambiente"]}.`);
  }
  const administrador = { usuario: opcoes["admin-usuario"], senha };
  const dados =
    opcoes.reutilizar === "true"
      ? await reutilizarDados({ cliente, administrador })
      : await prepararDados({
          cliente,
          administrador,
          usuarios: Number(opcoes.usuarios),
          banco: paraBanco(numeros(opcoes.banco)),
          sorteio: paraSorteio(numeros(opcoes.sorteio)),
        });
  console.log(
    `Dados: ${dados.colaboradores.length} pessoas, lição ${dados.licaoId} (limite de requisições atingido ${dados.limitesAtingidosNoPreparo}x no preparo)`,
  );
  return dados;
}

async function executar() {
  const opcoes = lerOpcoes(process.argv.slice(2));
  const usuarios = Number(opcoes.usuarios);
  const duracaoMs = Number(opcoes.duracao) * 1000;
  const [pensarMinimo, pensarMaximo] = numeros(opcoes.pensar);
  const pensarMs = [pensarMinimo, pensarMaximo];
  const sorteio = numeros(opcoes.sorteio);
  const totalEsperado = sorteio.reduce((soma, quantidade) => soma + quantidade, 0);
  const cliente = criarCliente(opcoes.base, { certificadoAutoassinado: opcoes.insegura === "true" });
  console.log(`Alvo: ${opcoes.base} | usuários: ${usuarios} | sorteio por pessoa: ${sorteio.join("/")} (${totalEsperado})`);

  const dados = opcoes["sem-dados"] === "true" ? { colaboradores: [] } : await criarDadosDeTeste(cliente, opcoes);

  const contexto = { cliente, dados, usuarios, duracaoMs, pensarMs, totalEsperado, rampaMs: Number(opcoes.rampa) * 1000 };
  const contextoDoLimite = {
    cliente,
    requisicoes: Number(opcoes["requisicoes-limite"]),
    concorrencia: Number(opcoes["concorrencia-limite"]),
  };
  const cenarios = {
    leitura: (metricas) => cenarioLeitura({ ...contexto, metricas, concorrencia: usuarios }),
    login: (metricas) => cenarioLoginEmRajada({ ...contexto, metricas }),
    licao: (metricas) => cenarioLicaoCompleta({ ...contexto, metricas }),
    continuo: (metricas) => cenarioContinuo({ ...contexto, metricas }),
    "forca-bruta": (metricas) =>
      cenarioForcaBrutaDePin({
        cliente,
        dados,
        metricas,
        tentativas: Number(opcoes["tentativas-forca-bruta"]),
        concorrencia: Number(opcoes["concorrencia-forca-bruta"]),
      }),
    limite: (metricas) => cenarioLimiteDeRequisicoes({ ...contextoDoLimite, metricas }),
    "limite-falso": (metricas) => cenarioLimiteComEnderecoFalso({ ...contextoDoLimite, metricas }),
  };

  for (const nome of opcoes.cenarios.split(",").filter(Boolean)) {
    if (!cenarios[nome]) {
      throw new Error(`Cenário desconhecido: ${nome}`);
    }
    await medirCenario(`Cenário ${nome}`, cenarios[nome]);
  }
  cliente.encerrar();
}

executar().catch((erro) => {
  console.error(erro.message);
  process.exitCode = 1;
});

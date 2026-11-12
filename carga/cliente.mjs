import http from "node:http";
import https from "node:https";

const TEMPO_LIMITE_PADRAO_MS = 30_000;
const CONEXOES_PADRAO = 600;

export function criarCliente(enderecoBase, opcoes = {}) {
  const {
    certificadoAutoassinado = false,
    conexoes = CONEXOES_PADRAO,
    tempoLimiteMs = TEMPO_LIMITE_PADRAO_MS,
  } = opcoes;
  const base = new URL(enderecoBase);
  const protocolo = base.protocol === "https:" ? https : http;
  const agente = new protocolo.Agent({
    keepAlive: true,
    maxSockets: conexoes,
    rejectUnauthorized: !certificadoAutoassinado,
  });

  function requisitar({ metodo = "GET", caminho, corpo, cookies = {}, cabecalhos: extras = {} }) {
    const inicio = performance.now();
    const conteudo = corpo === undefined ? null : JSON.stringify(corpo);
    return new Promise((resolver) => {
      const pedido = protocolo.request(
        {
          agent: agente,
          hostname: base.hostname,
          port: base.port,
          path: caminho,
          method: metodo,
          timeout: tempoLimiteMs,
          headers: { ...cabecalhos(conteudo, cookies), ...extras },
        },
        (resposta) => lerResposta(resposta, inicio, resolver),
      );
      pedido.on("timeout", () => pedido.destroy(new Error("tempo esgotado")));
      pedido.on("error", (erro) => resolver(resultadoComErro(inicio, erro)));
      if (conteudo !== null) {
        pedido.write(conteudo);
      }
      pedido.end();
    });
  }

  return { requisitar, encerrar: () => agente.destroy() };
}

function cabecalhos(conteudo, cookies) {
  const cabecalhosDoPedido = { accept: "application/json" };
  if (conteudo !== null) {
    cabecalhosDoPedido["content-type"] = "application/json";
    cabecalhosDoPedido["content-length"] = Buffer.byteLength(conteudo);
  }
  const texto = Object.entries(cookies)
    .map(([nome, valor]) => `${nome}=${valor}`)
    .join("; ");
  if (texto !== "") {
    cabecalhosDoPedido.cookie = texto;
  }
  return cabecalhosDoPedido;
}

function lerResposta(resposta, inicio, resolver) {
  const pedacos = [];
  resposta.on("data", (pedaco) => pedacos.push(pedaco));
  resposta.on("error", (erro) => resolver(resultadoComErro(inicio, erro)));
  resposta.on("end", () => {
    const texto = Buffer.concat(pedacos).toString("utf8");
    resolver({
      status: resposta.statusCode,
      corpo: converterJson(texto),
      cookies: extrairCookies(resposta.headers["set-cookie"]),
      ms: performance.now() - inicio,
      erro: null,
    });
  });
}

function resultadoComErro(inicio, erro) {
  return { status: 0, corpo: null, cookies: {}, ms: performance.now() - inicio, erro: erro.message };
}

function converterJson(texto) {
  if (texto === "") {
    return null;
  }
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

function extrairCookies(cabecalhoSetCookie = []) {
  const cookies = {};
  for (const linha of cabecalhoSetCookie) {
    const [nomeEValor] = linha.split(";");
    const separador = nomeEValor.indexOf("=");
    cookies[nomeEValor.slice(0, separador)] = nomeEValor.slice(separador + 1);
  }
  return cookies;
}

export function criarSessaoVirtual(cliente) {
  const cookies = {};

  async function requisitar(pedido) {
    const resultado = await cliente.requisitar({ ...pedido, cookies });
    Object.assign(cookies, resultado.cookies);
    return resultado;
  }

  return { requisitar };
}

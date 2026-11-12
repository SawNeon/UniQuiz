const PERCENTIS = [50, 95, 99];

export function criarMetricas() {
  const amostras = new Map();
  const inicio = performance.now();

  function registrar(rotulo, resultado) {
    if (!amostras.has(rotulo)) {
      amostras.set(rotulo, []);
    }
    amostras.get(rotulo).push(resultado);
  }

  function registrarFalhaDeVerificacao(rotulo) {
    registrar(rotulo, { status: -1, ms: 0, erro: "verificação falhou" });
  }

  function resumo() {
    const duracaoS = (performance.now() - inicio) / 1000;
    return [...amostras.entries()].map(([rotulo, resultados]) => resumirRotulo(rotulo, resultados, duracaoS));
  }

  return { registrar, registrarFalhaDeVerificacao, resumo };
}

function resumirRotulo(rotulo, resultados, duracaoS) {
  const duracoes = resultados.map((resultado) => resultado.ms).sort((a, b) => a - b);
  const porStatus = {};
  for (const { status } of resultados) {
    porStatus[status] = (porStatus[status] ?? 0) + 1;
  }
  const sucessos = resultados.filter((resultado) => resultado.status >= 200 && resultado.status < 300).length;
  const resumoDoRotulo = {
    rotulo,
    total: resultados.length,
    sucessos,
    erros: resultados.length - sucessos,
    porStatus,
    porSegundo: resultados.length / duracaoS,
    max: duracoes.at(-1) ?? 0,
  };
  for (const percentil of PERCENTIS) {
    resumoDoRotulo[`p${percentil}`] = percentilDe(duracoes, percentil);
  }
  return resumoDoRotulo;
}

function percentilDe(duracoesOrdenadas, percentil) {
  if (duracoesOrdenadas.length === 0) {
    return 0;
  }
  const indice = Math.min(duracoesOrdenadas.length - 1, Math.ceil((percentil / 100) * duracoesOrdenadas.length) - 1);
  return duracoesOrdenadas[Math.max(0, indice)];
}

export function formatarTabela(titulo, linhas) {
  const cabecalho = ["Operação", "Total", "OK", "Erros", "p50 ms", "p95 ms", "p99 ms", "Máx ms", "Req/s", "Status"];
  const corpo = linhas.map((linha) => [
    linha.rotulo,
    linha.total,
    linha.sucessos,
    linha.erros,
    arredondar(linha.p50),
    arredondar(linha.p95),
    arredondar(linha.p99),
    arredondar(linha.max),
    linha.porSegundo.toFixed(1),
    Object.entries(linha.porStatus)
      .map(([status, quantidade]) => `${status}×${quantidade}`)
      .join(" "),
  ]);
  const linhasDaTabela = [cabecalho, ...corpo].map((celulas) => `| ${celulas.join(" | ")} |`);
  linhasDaTabela.splice(1, 0, `|${cabecalho.map(() => "---").join("|")}|`);
  return `\n### ${titulo}\n\n${linhasDaTabela.join("\n")}\n`;
}

function arredondar(numero) {
  return Math.round(numero);
}

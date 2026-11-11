import type {
  ConteudoAdmin,
  DadosDoDesafio,
  DesafioPublico,
  RespostaCompletarClicando,
  RespostaDoColaborador,
  RespostaMultiplaEscolha,
  RespostaVerdadeiroOuFalso,
} from "../../api/tipos";

export function paraPublico(dados: DadosDoDesafio): DesafioPublico {
  const base = { id: 0, enunciado: dados.enunciado, dica: dados.dica, dificuldade: dados.dificuldade };
  switch (dados.tipo) {
    case "MULTIPLA_ESCOLHA":
      return {
        ...base,
        tipo: dados.tipo,
        conteudo: {
          alternativas: dados.conteudo.alternativas.map(({ id, texto }) => ({ id, texto })),
        },
      };
    case "VERDADEIRO_OU_FALSO":
      return { ...base, tipo: dados.tipo, conteudo: { afirmacao: dados.conteudo.afirmacao } };
    case "COMPLETAR_CLICANDO":
      return {
        ...base,
        tipo: dados.tipo,
        conteudo: {
          texto: dados.conteudo.texto,
          lacunas: dados.conteudo.lacunas.map((lacuna) => ({
            id: lacuna.id,
            palavras: lacuna.banco.map((opcao) => opcao.palavra).filter(Boolean),
          })),
        },
      };
  }
}

export function acertouNaPrevia(dados: ConteudoAdmin, resposta: RespostaDoColaborador): boolean {
  switch (dados.tipo) {
    case "MULTIPLA_ESCOLHA": {
      const escolhida = (resposta as RespostaMultiplaEscolha).alternativaId;
      return dados.conteudo.alternativas.some((alternativa) => alternativa.id === escolhida && alternativa.correta);
    }
    case "VERDADEIRO_OU_FALSO":
      return (resposta as RespostaVerdadeiroOuFalso).valor === dados.conteudo.verdadeira;
    case "COMPLETAR_CLICANDO": {
      const preenchidas = (resposta as RespostaCompletarClicando).lacunas;
      return dados.conteudo.lacunas.every(
        (lacuna) => lacuna.banco.find((opcao) => opcao.correta)?.palavra === preenchidas[String(lacuna.id)],
      );
    }
  }
}

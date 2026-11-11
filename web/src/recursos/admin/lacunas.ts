import type { ConteudoCompletarClicandoAdmin } from "../../api/tipos";

const MARCADOR = /\{\{(\d+)\}\}/;

export function transformarEmLacuna(
  conteudo: ConteudoCompletarClicandoAdmin,
  inicio: number,
  fim: number,
): ConteudoCompletarClicandoAdmin | { erro: string } {
  const selecionado = conteudo.texto.slice(inicio, fim);
  const palavra = selecionado.trim();
  if (!palavra) {
    return { erro: "Selecione com o mouse a palavra que vai virar lacuna." };
  }
  if (MARCADOR.test(selecionado)) {
    return { erro: "A seleção já contém uma lacuna. Selecione só a palavra." };
  }
  const deslocamentoInicial = selecionado.indexOf(palavra);
  const inicioDaPalavra = inicio + deslocamentoInicial;
  const fimDaPalavra = inicioDaPalavra + palavra.length;
  const id = conteudo.lacunas.reduce((maior, lacuna) => Math.max(maior, lacuna.id), 0) + 1;

  return {
    texto: `${conteudo.texto.slice(0, inicioDaPalavra)}{{${id}}}${conteudo.texto.slice(fimDaPalavra)}`,
    lacunas: [...conteudo.lacunas, { id, banco: [{ palavra, correta: true, motivo: "" }] }],
  };
}

export function desfazerLacuna(conteudo: ConteudoCompletarClicandoAdmin, id: number): ConteudoCompletarClicandoAdmin {
  const lacuna = conteudo.lacunas.find((existente) => existente.id === id);
  const palavraCerta = lacuna?.banco.find((opcao) => opcao.correta)?.palavra ?? "";
  return {
    texto: conteudo.texto.replace(`{{${id}}}`, palavraCerta),
    lacunas: conteudo.lacunas.filter((existente) => existente.id !== id),
  };
}

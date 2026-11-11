import { desfazerLacuna, transformarEmLacuna } from "./lacunas";

const frase = "O registro deve ser feito pelo responsável do processo.";

describe("transformarEmLacuna", () => {
  it("troca a palavra selecionada por um marcador e guarda a resposta certa", () => {
    const inicio = frase.indexOf("responsável");
    const resultado = transformarEmLacuna({ texto: frase, lacunas: [] }, inicio, inicio + "responsável".length);

    expect(resultado).toEqual({
      texto: "O registro deve ser feito pelo {{1}} do processo.",
      lacunas: [{ id: 1, banco: [{ palavra: "responsável", correta: true, motivo: "" }] }],
    });
  });

  it("ignora espaços em volta da seleção e numera a próxima lacuna", () => {
    const primeira = transformarEmLacuna({ texto: frase, lacunas: [] }, 1, 11);
    if ("erro" in primeira) {
      throw new Error(primeira.erro);
    }
    const inicio = primeira.texto.indexOf(" processo");
    const segunda = transformarEmLacuna(primeira, inicio, inicio + " processo".length);

    expect(segunda).toMatchObject({ texto: "O {{1}} deve ser feito pelo responsável do {{2}}." });
  });

  it("recusa seleção vazia ou que já tem lacuna", () => {
    expect(transformarEmLacuna({ texto: frase, lacunas: [] }, 3, 3)).toHaveProperty("erro");
    expect(transformarEmLacuna({ texto: "A {{1}} b", lacunas: [] }, 0, 7)).toHaveProperty("erro");
  });
});

describe("desfazerLacuna", () => {
  it("devolve a palavra certa para a frase", () => {
    const conteudo = {
      texto: "Use ferramentas {{1}}.",
      lacunas: [{ id: 1, banco: [{ palavra: "aprovadas", correta: true, motivo: "" }] }],
    };

    expect(desfazerLacuna(conteudo, 1)).toEqual({ texto: "Use ferramentas aprovadas.", lacunas: [] });
  });
});

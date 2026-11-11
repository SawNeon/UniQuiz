import type { DadosDoDesafio } from "../../api/tipos";
import { acertouNaPrevia, paraPublico } from "./previa";

const base = {
  enunciado: "Enunciado",
  dificuldade: "FACIL" as const,
  pontos: 10,
  explicacao: "",
  dica: "Dica",
};

const multipla: DadosDoDesafio = {
  ...base,
  tipo: "MULTIPLA_ESCOLHA",
  conteudo: {
    alternativas: [
      { id: "a", texto: "Certa", correta: true, motivo: "" },
      { id: "b", texto: "Errada", correta: false, motivo: "Porque não." },
    ],
  },
};

const verdadeiroOuFalso: DadosDoDesafio = {
  ...base,
  tipo: "VERDADEIRO_OU_FALSO",
  conteudo: { afirmacao: "Afirmação", verdadeira: false, motivoDoErro: "Motivo" },
};

const completar: DadosDoDesafio = {
  ...base,
  tipo: "COMPLETAR_CLICANDO",
  conteudo: {
    texto: "Use {{1}}.",
    lacunas: [
      {
        id: 1,
        banco: [
          { palavra: "aprovadas", correta: true, motivo: "" },
          { palavra: "", correta: false, motivo: "" },
          { palavra: "gratuitas", correta: false, motivo: "x" },
        ],
      },
    ],
  },
};

describe("pré-visualização do admin", () => {
  it("monta a versão do colaborador sem gabarito", () => {
    expect(paraPublico(multipla)).toEqual({
      id: 0,
      enunciado: "Enunciado",
      dica: "Dica",
      dificuldade: "FACIL",
      tipo: "MULTIPLA_ESCOLHA",
      conteudo: {
        alternativas: [
          { id: "a", texto: "Certa" },
          { id: "b", texto: "Errada" },
        ],
      },
    });
    expect(paraPublico(verdadeiroOuFalso).conteudo).toEqual({ afirmacao: "Afirmação" });
    expect(paraPublico(completar).conteudo).toEqual({
      texto: "Use {{1}}.",
      lacunas: [{ id: 1, palavras: ["aprovadas", "gratuitas"] }],
    });
  });

  it("confere a resposta da prévia em cada tipo", () => {
    expect(acertouNaPrevia(multipla, { alternativaId: "a" })).toBe(true);
    expect(acertouNaPrevia(multipla, { alternativaId: "b" })).toBe(false);
    expect(acertouNaPrevia(verdadeiroOuFalso, { valor: false })).toBe(true);
    expect(acertouNaPrevia(verdadeiroOuFalso, { valor: true })).toBe(false);
    expect(acertouNaPrevia(completar, { lacunas: { "1": "aprovadas" } })).toBe(true);
    expect(acertouNaPrevia(completar, { lacunas: { "1": "gratuitas" } })).toBe(false);
  });
});

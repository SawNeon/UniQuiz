import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { Correcao, ResultadoDaLicao, TentativaIniciada } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDaLicao } from "./TelaDaLicao";

const tentativa: TentativaIniciada = {
  tentativaId: 9,
  titulo: "IA no trabalho",
  valePontosIntegrais: true,
  desafios: [
    {
      id: 1,
      tipo: "VERDADEIRO_OU_FALSO",
      enunciado: "Verdadeiro ou falso?",
      dificuldade: "FACIL",
      conteudo: { afirmacao: "A IA pode errar com confiança." },
    },
    {
      id: 2,
      tipo: "MULTIPLA_ESCOLHA",
      enunciado: "O que fazer antes de colar um texto numa IA?",
      dica: "Pense no que identifica uma pessoa.",
      dificuldade: "MEDIO",
      conteudo: {
        alternativas: [
          { id: "a", texto: "Tirar dados pessoais" },
          { id: "b", texto: "Corrigir a digitação" },
        ],
      },
    },
  ],
};

function correcao(parcial: Partial<Correcao>): Correcao {
  return {
    correta: true,
    respostaCerta: "",
    explicacao: "",
    pontos: 0,
    bonusDeSequencia: 0,
    segundaChance: false,
    desafiosParaSegundaChance: [],
    ...parcial,
  };
}

const resultado: ResultadoDaLicao = {
  acertos: 1,
  total: 2,
  perfeita: false,
  tempoMs: 65_000,
  ganhos: [
    { motivo: "ACERTOS", pontos: 10 },
    { motivo: "PRIMEIRA_LICAO_DO_DIA", pontos: 10 },
  ],
  pontosGanhos: 20,
  diasSeguidos: 1,
  trofeusNovos: [
    {
      codigo: "PRIMEIRO_PASSO",
      nome: "Primeiro passo",
      descricao: "Concluiu a primeira lição.",
      icone: "passo",
    },
  ],
  moduloConcluido: { id: 20, titulo: "IA com responsabilidade" },
};

describe("TelaDaLicao", () => {
  it("faz a lição inteira, com retorno, segunda chance e resultado", async () => {
    let respostas = 0;
    const chamadas = simularApi({
      "POST /licoes/7/tentativas": { status: 201, corpo: tentativa },
      "POST /tentativas/9/respostas": () => {
        respostas += 1;
        if (respostas === 1) {
          return { corpo: correcao({ correta: true, pontos: 10, explicacao: "Sempre confira." }) };
        }
        if (respostas === 2) {
          return {
            corpo: correcao({
              correta: false,
              motivoDoErro: "Digitação não expõe ninguém.",
              respostaCerta: "Tirar dados pessoais",
              explicacao: "O que entra pode sair da empresa.",
              desafiosParaSegundaChance: [2],
            }),
          };
        }
        return { corpo: correcao({ correta: true, segundaChance: true, explicacao: "Agora sim." }) };
      },
      "POST /tentativas/9/concluir": { corpo: resultado },
      "POST /modulos/20/certificado": {
        corpo: {
          codigo: "ABCD2345EFGH",
          moduloId: 20,
          modulo: "IA com responsabilidade",
          emitidoEm: "2026-11-10",
        },
      },
    });
    renderizar(<TelaDaLicao />, { rota: "/licoes/7", caminho: "/licoes/:licaoId" });

    expect(await screen.findByText("A IA pode errar com confiança.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Verificar" })).toBeDisabled();
    await userEvent.click(screen.getByRole("radio", { name: "Verdadeiro" }));
    await userEvent.click(screen.getByRole("button", { name: "Verificar" }));
    expect(await screen.findByText("Muito bem!")).toBeInTheDocument();
    expect(screen.getByText("+10 pontos")).toBeInTheDocument();
    expect(screen.getByRole("progressbar")).toHaveAttribute("aria-valuenow", "50");

    await userEvent.click(screen.getByRole("button", { name: "Continuar" }));
    expect(screen.getByText("Dica: Pense no que identifica uma pessoa.")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("radio", { name: "Corrigir a digitação" }));
    await userEvent.click(screen.getByRole("button", { name: "Verificar" }));
    expect(await screen.findByText("Quase!")).toBeInTheDocument();
    expect(screen.getByText("Digitação não expõe ninguém.")).toBeInTheDocument();
    expect(screen.getByText("Tirar dados pessoais", { selector: "p" })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Continuar" }));
    expect(screen.getByText(/segunda chance/i)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("radio", { name: "Tirar dados pessoais" }));
    await userEvent.click(screen.getByRole("button", { name: "Verificar" }));
    expect(
      await screen.findByText("Segunda chance: não vale pontos, mas vale aprender."),
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Continuar" }));
    expect(await screen.findByRole("heading", { name: "Lição concluída!" })).toBeInTheDocument();
    expect(screen.getByText("+20")).toBeInTheDocument();
    expect(screen.getByText("1/2")).toBeInTheDocument();
    expect(screen.getByText("1 min 5 s")).toBeInTheDocument();
    expect(screen.getByText("Troféu novo: Primeiro passo")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Gerar meu certificado" }));
    expect(await screen.findByRole("link", { name: "Baixar certificado" })).toHaveAttribute(
      "href",
      "/api/certificados/ABCD2345EFGH/pdf",
    );

    const enviadas = chamadas.filter((chamada) => chamada.caminho === "/tentativas/9/respostas");
    expect(enviadas.map((chamada) => chamada.corpo)).toEqual([
      { desafioId: 1, resposta: { valor: true } },
      { desafioId: 2, resposta: { alternativaId: "b" } },
      { desafioId: 2, resposta: { alternativaId: "a" } },
    ]);
  });

  it("avisa quando a lição já foi feita e vale menos pontos", async () => {
    simularApi({
      "POST /licoes/7/tentativas": { status: 201, corpo: { ...tentativa, valePontosIntegrais: false } },
    });
    renderizar(<TelaDaLicao />, { rota: "/licoes/7", caminho: "/licoes/:licaoId" });

    expect(await screen.findByText(/Você já fez esta lição/)).toBeInTheDocument();
  });

  it("mostra quando a lição ainda está bloqueada", async () => {
    simularApi({
      "POST /licoes/7/tentativas": {
        status: 422,
        corpo: { detail: "Conclua as lições anteriores para liberar esta." },
      },
    });
    renderizar(<TelaDaLicao />, { rota: "/licoes/7", caminho: "/licoes/:licaoId" });

    expect(await screen.findByRole("alert")).toHaveTextContent("Conclua as lições anteriores");
  });

  it("sai da lição pelo botão de fechar", async () => {
    simularApi({ "POST /licoes/7/tentativas": { status: 201, corpo: tentativa } });
    renderizar(<TelaDaLicao />, { rota: "/licoes/7", caminho: "/licoes/:licaoId" });

    await userEvent.click(screen.getByRole("button", { name: "Sair da lição" }));

    expect(await screen.findByText("Página: /")).toBeInTheDocument();
  });
});

import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { DesafioDoTipo } from "./contrato";
import { ComponenteDoDesafio } from "./ComponenteDoDesafio";
import { MultiplaEscolha } from "./MultiplaEscolha";
import { respostaCompleta } from "./respostaCompleta";
import { VerdadeiroOuFalso } from "./VerdadeiroOuFalso";

const multipla: DesafioDoTipo<"MULTIPLA_ESCOLHA"> = {
  id: 1,
  tipo: "MULTIPLA_ESCOLHA",
  enunciado: "O que fazer antes de usar IA?",
  dificuldade: "FACIL",
  conteudo: {
    alternativas: [
      { id: "a", texto: "Tirar dados pessoais" },
      { id: "b", texto: "Corrigir a digitação" },
      { id: "c", texto: "Traduzir" },
    ],
  },
};

const verdadeiroOuFalso: DesafioDoTipo<"VERDADEIRO_OU_FALSO"> = {
  id: 2,
  tipo: "VERDADEIRO_OU_FALSO",
  enunciado: "Verdadeiro ou falso?",
  dificuldade: "MEDIO",
  conteudo: { afirmacao: "Dados de saúde são sensíveis." },
};

describe("MultiplaEscolha", () => {
  it("escolhe a alternativa tocada", async () => {
    const aoResponder = vi.fn();
    render(
      <MultiplaEscolha
        desafio={multipla}
        resposta={null}
        aoResponder={aoResponder}
        correcao={null}
        desabilitado={false}
      />,
    );

    await userEvent.click(screen.getByRole("radio", { name: "Corrigir a digitação" }));

    expect(aoResponder).toHaveBeenCalledWith({ alternativaId: "b" });
  });

  it("navega pelas setas do teclado, dando a volta no fim", () => {
    const aoResponder = vi.fn();
    render(
      <MultiplaEscolha
        desafio={multipla}
        resposta={{ alternativaId: "c" }}
        aoResponder={aoResponder}
        correcao={null}
        desabilitado={false}
      />,
    );

    fireEvent.keyDown(screen.getByRole("radiogroup"), { key: "ArrowDown" });
    expect(aoResponder).toHaveBeenLastCalledWith({ alternativaId: "a" });
    fireEvent.keyDown(screen.getByRole("radiogroup"), { key: "ArrowUp" });
    expect(aoResponder).toHaveBeenLastCalledWith({ alternativaId: "b" });
  });

  it("marca a escolhida depois da correção e bloqueia novas escolhas", async () => {
    const aoResponder = vi.fn();
    render(
      <MultiplaEscolha
        desafio={multipla}
        resposta={{ alternativaId: "b" }}
        aoResponder={aoResponder}
        correcao={{ correta: false }}
        desabilitado
      />,
    );

    expect(screen.getByRole("radio", { name: "Corrigir a digitação" })).toHaveAttribute(
      "aria-checked",
      "true",
    );
    expect(screen.getByRole("radio", { name: "Corrigir a digitação" }).className).toContain("bg-erro-suave");
    fireEvent.keyDown(screen.getByRole("radiogroup"), { key: "ArrowDown" });
    await userEvent.click(screen.getByRole("radio", { name: "Traduzir" }));
    expect(aoResponder).not.toHaveBeenCalled();
  });
});

describe("VerdadeiroOuFalso", () => {
  it("mostra a afirmação e responde com o valor", async () => {
    const aoResponder = vi.fn();
    render(
      <VerdadeiroOuFalso
        desafio={verdadeiroOuFalso}
        resposta={{ valor: true }}
        aoResponder={aoResponder}
        correcao={null}
        desabilitado={false}
      />,
    );

    expect(screen.getByText("Dados de saúde são sensíveis.")).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "Verdadeiro" })).toHaveAttribute("aria-checked", "true");
    await userEvent.click(screen.getByRole("radio", { name: "Falso" }));
    expect(aoResponder).toHaveBeenCalledWith({ valor: false });
  });
});

describe("ComponenteDoDesafio", () => {
  it("escolhe o componente pelo tipo do desafio", () => {
    const { rerender } = render(
      <ComponenteDoDesafio
        desafio={multipla}
        resposta={null}
        aoResponder={vi.fn()}
        correcao={null}
        desabilitado={false}
      />,
    );
    expect(screen.getAllByRole("radio")).toHaveLength(3);

    rerender(
      <ComponenteDoDesafio
        desafio={verdadeiroOuFalso}
        resposta={null}
        aoResponder={vi.fn()}
        correcao={null}
        desabilitado={false}
      />,
    );
    expect(screen.getByRole("radio", { name: "Falso" })).toBeInTheDocument();
  });
});

describe("respostaCompleta", () => {
  const completar: DesafioDoTipo<"COMPLETAR_CLICANDO"> = {
    id: 3,
    tipo: "COMPLETAR_CLICANDO",
    enunciado: "Complete",
    dificuldade: "FACIL",
    conteudo: {
      texto: "{{1}} e {{2}}",
      lacunas: [
        { id: 1, palavras: ["a"] },
        { id: 2, palavras: ["b"] },
      ],
    },
  };

  it("exige todas as lacunas preenchidas", () => {
    expect(respostaCompleta(completar, null)).toBe(false);
    expect(respostaCompleta(completar, { lacunas: { "1": "a" } })).toBe(false);
    expect(respostaCompleta(completar, { lacunas: { "1": "a", "2": "b" } })).toBe(true);
  });

  it("para os outros tipos basta ter escolhido", () => {
    expect(respostaCompleta(multipla, null)).toBe(false);
    expect(respostaCompleta(multipla, { alternativaId: "a" })).toBe(true);
  });
});

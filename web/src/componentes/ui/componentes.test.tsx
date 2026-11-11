import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Mascote } from "../../mascote/Mascote";
import { Botao } from "./Botao";
import { Carregando, MensagemDeErro } from "./Estados";
import { Icone, IconeDoTrofeu } from "./Icone";
import { TextoFormatado } from "./TextoFormatado";

describe("TextoFormatado", () => {
  it("formata títulos, negrito e listas", () => {
    render(<TextoFormatado conteudo={"# Título\n\nTexto com **destaque**.\n\n- um\n- dois"} />);

    expect(screen.getByRole("heading", { name: "Título" })).toBeInTheDocument();
    expect(screen.getByText("destaque").tagName).toBe("STRONG");
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
  });

  it("não executa HTML nem cria links ou imagens", () => {
    const { container } = render(
      <TextoFormatado
        conteudo={
          '<script>alert(1)</script>\n\n<img src=x onerror="alert(1)">\n\n[clique](https://golpe.exemplo)\n\n![foto](https://x/y.png)'
        }
      />,
    );

    expect(container.querySelector("script, img, a")).toBeNull();
    expect(screen.getByText("clique")).toBeInTheDocument();
  });
});

describe("componentes básicos", () => {
  it("botão principal é do tipo button por padrão e respeita desabilitado", async () => {
    const aoClicar = vi.fn();
    render(
      <Botao onClick={aoClicar} disabled>
        Verificar
      </Botao>,
    );

    expect(screen.getByRole("button", { name: "Verificar" })).toHaveAttribute("type", "button");
    await userEvent.click(screen.getByRole("button", { name: "Verificar" }));
    expect(aoClicar).not.toHaveBeenCalled();
  });

  it("estados de carregamento e erro são anunciados", async () => {
    const aoTentar = vi.fn();
    render(
      <>
        <Carregando texto="Somando seus pontos…" />
        <MensagemDeErro erro={new Error("Sem conexão.")} aoTentarDeNovo={aoTentar} />
      </>,
    );

    expect(screen.getByRole("status")).toHaveTextContent("Somando seus pontos…");
    expect(screen.getByRole("alert")).toHaveTextContent("Sem conexão.");
    await userEvent.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(aoTentar).toHaveBeenCalled();
  });

  it("ícones são decorativos e o ícone de troféu desconhecido cai no troféu padrão", () => {
    const { container } = render(
      <>
        <Icone nome="certo" />
        <IconeDoTrofeu icone="nao-existe" />
      </>,
    );

    expect(container.querySelectorAll('svg[aria-hidden="true"]')).toHaveLength(2);
  });

  it("o mascote tem nome acessível em cada estado", () => {
    render(
      <>
        <Mascote estado="neutro" />
        <Mascote estado="feliz" />
        <Mascote estado="triste" />
        <Mascote estado="comemorando" />
      </>,
    );

    for (const nome of ["Bit, o mascote da TI", "Bit feliz", "Bit triste", "Bit comemorando"]) {
      expect(screen.getByRole("img", { name: nome })).toBeInTheDocument();
    }
  });
});

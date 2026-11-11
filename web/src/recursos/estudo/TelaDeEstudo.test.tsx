import { screen } from "@testing-library/react";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDeEstudo } from "./TelaDeEstudo";

describe("TelaDeEstudo", () => {
  it("mostra o material do módulo e leva para a próxima lição disponível", async () => {
    simularApi({
      "GET /modulos/10/estudo": {
        corpo: { moduloId: 10, titulo: "LGPD", conteudo: "# LGPD na prática\n\nProtege **dados pessoais**." },
      },
      "GET /caminho": {
        corpo: [
          {
            id: 10,
            titulo: "LGPD",
            descricao: "",
            temMaterial: true,
            concluido: false,
            licoesConcluidas: 0,
            licoes: [{ id: 1, titulo: "O básico da LGPD", situacao: "DISPONIVEL" }],
          },
        ],
      },
    });
    renderizar(<TelaDeEstudo />, { rota: "/modulos/10/estudo", caminho: "/modulos/:moduloId/estudo" });

    expect(await screen.findByRole("heading", { name: "LGPD na prática" })).toBeInTheDocument();
    expect(screen.getByText("dados pessoais").tagName).toBe("STRONG");
    expect(await screen.findByRole("link", { name: "Fazer a lição: O básico da LGPD" })).toHaveAttribute(
      "href",
      "/licoes/1",
    );
    expect(screen.getByRole("link", { name: "Voltar ao caminho" })).toHaveAttribute("href", "/");
  });

  it("avisa quando o módulo não tem material", async () => {
    simularApi({
      "GET /modulos/10/estudo": {
        status: 404,
        corpo: { detail: "Este módulo ainda não tem material de estudo." },
      },
      "GET /caminho": { corpo: [] },
    });
    renderizar(<TelaDeEstudo />, { rota: "/modulos/10/estudo", caminho: "/modulos/:moduloId/estudo" });

    expect(await screen.findByRole("alert")).toHaveTextContent("ainda não tem material");
  });
});

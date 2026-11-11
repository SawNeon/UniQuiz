import { screen } from "@testing-library/react";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDeValidacao } from "./TelaDeValidacao";

describe("TelaDeValidacao", () => {
  it("confirma um certificado válido", async () => {
    simularApi({
      "GET /publico/certificados/ABCD-2345-EFGH": {
        corpo: {
          codigo: "ABCD-2345-EFGH",
          nome: "Ana Exemplo",
          modulo: "LGPD na prática",
          emitidoEm: "2026-12-18",
        },
      },
    });
    renderizar(<TelaDeValidacao />, { rota: "/certificado/ABCD-2345-EFGH", caminho: "/certificado/:codigo" });

    expect(await screen.findByText("Certificado válido")).toBeInTheDocument();
    expect(screen.getByText("Ana Exemplo")).toBeInTheDocument();
    expect(screen.getByText("LGPD na prática")).toBeInTheDocument();
    expect(screen.getByText(/18\/12\/2026/)).toBeInTheDocument();
  });

  it("avisa quando o código não existe", async () => {
    simularApi({
      "GET /publico/certificados/ERRADO": { status: 404, corpo: { detail: "Certificado não encontrado." } },
    });
    renderizar(<TelaDeValidacao />, { rota: "/certificado/ERRADO", caminho: "/certificado/:codigo" });

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não encontramos um certificado com o código ERRADO",
    );
  });
});

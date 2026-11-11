import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDeEntrada } from "./TelaDeEntrada";

function apiDaEntrada(sessao: { status?: number; corpo?: unknown } = {}) {
  return simularApi({
    "GET /empresas": { corpo: [{ id: 1, nome: "Unimed Exemplo" }] },
    "GET /empresas/1/setores": { corpo: [{ id: 2, nome: "Tecnologia da Informação" }] },
    "GET /setores/2/colaboradores": {
      corpo: [
        { id: 3, nome: "Ana Exemplo" },
        { id: 4, nome: "Bruno Teste" },
        { id: 5, nome: "João Ávila" },
      ],
    },
    "POST /sessao": sessao,
  });
}

async function escolherSetor() {
  await userEvent.selectOptions(
    screen.getByLabelText("Empresa"),
    await screen.findByRole("option", { name: "Unimed Exemplo" }),
  );
  await userEvent.selectOptions(
    screen.getByLabelText("Setor"),
    await screen.findByRole("option", { name: "Tecnologia da Informação" }),
  );
}

describe("TelaDeEntrada", () => {
  it("entra depois de escolher empresa, setor, nome e confirmar", async () => {
    const chamadas = apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });

    expect(screen.getByLabelText("Setor")).toBeDisabled();
    await escolherSetor();
    await userEvent.click(await screen.findByRole("button", { name: "Bruno Teste" }));

    expect(
      screen.getByRole("heading", { name: "Você é Bruno Teste, do setor Tecnologia da Informação?" }),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Sim, sou eu" }));

    expect(await screen.findByText("Página: /")).toBeInTheDocument();
    expect(chamadas).toContainEqual({ metodo: "POST", caminho: "/sessao", corpo: { colaboradorId: 4 } });
  });

  it("busca o nome ignorando acentos e maiúsculas", async () => {
    apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();

    await userEvent.type(await screen.findByLabelText("Seu nome"), "joao avi");

    expect(screen.getByRole("button", { name: "João Ávila" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Ana Exemplo" })).not.toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText("Seu nome"));
    await userEvent.type(screen.getByLabelText("Seu nome"), "ninguém");
    expect(screen.getByText("Nenhum nome encontrado neste setor.")).toBeInTheDocument();
  });

  it("permite voltar da confirmação e mostra o erro do servidor", async () => {
    apiDaEntrada({
      status: 422,
      corpo: { detail: "Não encontramos esse nome entre os colaboradores ativos." },
    });
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();

    await userEvent.click(await screen.findByRole("button", { name: "Ana Exemplo" }));
    await userEvent.click(screen.getByRole("button", { name: "Sim, sou eu" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Não encontramos esse nome");

    await userEvent.click(screen.getByRole("button", { name: "Não, voltar" }));
    expect(screen.getByLabelText("Empresa")).toBeInTheDocument();
  });

  it("avisa o que é guardado, como pede a LGPD", () => {
    apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });

    expect(screen.getByText(/Guardamos seu nome, setor, empresa e pontos/)).toBeInTheDocument();
  });
});

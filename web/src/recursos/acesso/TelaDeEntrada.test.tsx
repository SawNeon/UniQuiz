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
        { id: 3, nome: "Ana Exemplo", temPin: false },
        { id: 4, nome: "Bruno Teste", temPin: true },
        { id: 5, nome: "João Ávila", temPin: false },
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
    expect(screen.getByText("Digite o seu PIN de 3 números.")).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("PIN"), "123");
    await userEvent.click(screen.getByRole("button", { name: "Sim, sou eu" }));

    expect(await screen.findByText("Página: /")).toBeInTheDocument();
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/sessao",
      corpo: { colaboradorId: 4, pin: "123" },
    });
  });

  it("no primeiro acesso pede para criar um PIN de 3 números e só aceita números", async () => {
    const chamadas = apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();
    await userEvent.click(await screen.findByRole("button", { name: "Ana Exemplo" }));

    expect(screen.getByText(/primeiro acesso/)).toBeInTheDocument();
    const campo = screen.getByLabelText("Crie o seu PIN");
    const botao = screen.getByRole("button", { name: "Sim, sou eu" });
    expect(botao).toBeDisabled();

    await userEvent.type(campo, "a1b2");
    expect(campo).toHaveValue("12");
    expect(botao).toBeDisabled();

    await userEvent.type(campo, "3456");
    expect(campo).toHaveValue("123");
    await userEvent.click(botao);

    expect(await screen.findByText("Página: /")).toBeInTheDocument();
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/sessao",
      corpo: { colaboradorId: 3, pin: "123" },
    });
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
      corpo: { detail: "PIN incorreto." },
    });
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();

    await userEvent.click(await screen.findByRole("button", { name: "Bruno Teste" }));
    await userEvent.type(screen.getByLabelText("PIN"), "999");
    await userEvent.click(screen.getByRole("button", { name: "Sim, sou eu" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("PIN incorreto.");

    await userEvent.click(screen.getByRole("button", { name: "Não, voltar" }));
    expect(screen.getByLabelText("Empresa")).toBeInTheDocument();
    await userEvent.click(await screen.findByRole("button", { name: "Bruno Teste" }));
    expect(screen.getByLabelText("PIN")).toHaveValue("");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("avisa o que é guardado, como pede a LGPD", () => {
    apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });

    expect(screen.getByText(/Guardamos seu nome, setor, empresa e pontos/)).toBeInTheDocument();
  });
});

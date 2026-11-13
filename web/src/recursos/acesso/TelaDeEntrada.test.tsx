import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDeEntrada } from "./TelaDeEntrada";

const ana = { id: 3, nome: "Ana Exemplo", temPin: false };
const bruno = { id: 4, nome: "Bruno Teste", temPin: true };
const joao = { id: 5, nome: "João Ávila", temPin: false };

function apiDaEntrada(
  sessao: { status?: number; corpo?: unknown } = {},
  empresas = [{ id: 1, nome: "Unimed Exemplo" }],
) {
  return simularApi({
    "GET /empresas": { corpo: empresas },
    "GET /empresas/1/setores": {
      corpo: [
        { id: 2, nome: "Tecnologia da Informação" },
        { id: 6, nome: "Financeiro" },
      ],
    },
    "GET /empresas/7/setores": { corpo: [{ id: 8, nome: "Comercial" }] },
    "GET /setores/2/colaboradores?busca=ana": { corpo: [ana] },
    "GET /setores/2/colaboradores?busca=bru": { corpo: [bruno] },
    "GET /setores/2/colaboradores?busca=joao%20avi": { corpo: [joao] },
    "GET /setores/2/colaboradores?busca=ningu%C3%A9m": { corpo: [] },
    "GET /setores/8/colaboradores?busca=bru": { corpo: [bruno] },
    "POST /sessao": sessao,
  });
}

async function escolherSetor() {
  await userEvent.selectOptions(
    screen.getByLabelText("Setor"),
    await screen.findByRole("option", { name: "Tecnologia da Informação" }),
  );
}

async function buscar(texto: string) {
  const campo = await screen.findByLabelText("Seu nome");
  await userEvent.clear(campo);
  await userEvent.type(campo, texto);
}

describe("TelaDeEntrada", () => {
  beforeEach(() => window.localStorage.clear());

  it("escolhe sozinha a única empresa e entra depois de buscar o nome e confirmar", async () => {
    const chamadas = apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });

    expect(await screen.findByRole("option", { name: "Unimed Exemplo", selected: true })).toBeInTheDocument();
    await escolherSetor();
    await buscar("bru");
    await userEvent.click(await screen.findByRole("button", { name: "Bruno Teste" }));

    expect(
      screen.getByRole("heading", { name: "Você é Bruno Teste, do setor Tecnologia da Informação?" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Digite o seu PIN de 3 números.")).toBeInTheDocument();
    expect(screen.getByText("Esqueceu o PIN? Procure o administrador do UniQuiz.")).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("PIN"), "123");
    await userEvent.click(screen.getByRole("button", { name: "Sim, sou eu" }));

    expect(await screen.findByText("Página: /")).toBeInTheDocument();
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/sessao",
      corpo: { colaboradorId: 4, pin: "123", aparelhoCompartilhado: false },
    });
  });

  it("só procura nomes depois de 3 letras", async () => {
    const chamadas = apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();

    await buscar("an");
    expect(screen.getByText(/só aparecem depois de 3 letras/)).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "Nomes encontrados" })).not.toBeInTheDocument();

    await userEvent.type(screen.getByLabelText("Seu nome"), "a");
    expect(await screen.findByRole("button", { name: "Ana Exemplo" })).toBeInTheDocument();
    expect(chamadas.filter((chamada) => chamada.caminho.includes("colaboradores"))).toEqual([
      { metodo: "GET", caminho: "/setores/2/colaboradores?busca=ana", corpo: undefined },
    ]);
  });

  it("no primeiro acesso pede o PIN duas vezes e só aceita números", async () => {
    const chamadas = apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();
    await buscar("ana");
    await userEvent.click(await screen.findByRole("button", { name: "Ana Exemplo" }));

    expect(screen.getByText(/primeiro acesso/)).toBeInTheDocument();
    const campo = screen.getByLabelText("Crie o seu PIN");
    const repeticao = screen.getByLabelText("Repita o PIN");
    const botao = screen.getByRole("button", { name: "Sim, sou eu" });

    await userEvent.type(campo, "a1b2");
    expect(campo).toHaveValue("12");
    await userEvent.type(campo, "3456");
    expect(campo).toHaveValue("123");
    expect(botao).toBeDisabled();

    await userEvent.type(repeticao, "124");
    expect(screen.getByRole("alert")).toHaveTextContent("Os dois PINs precisam ser iguais.");
    expect(botao).toBeDisabled();

    await userEvent.clear(repeticao);
    await userEvent.type(repeticao, "123");
    await userEvent.click(screen.getByRole("checkbox", { name: /aparelho compartilhado/ }));
    await userEvent.click(botao);

    expect(await screen.findByText("Página: /")).toBeInTheDocument();
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/sessao",
      corpo: { colaboradorId: 3, pin: "123", aparelhoCompartilhado: true },
    });
  });

  it("procura o nome ignorando acentos e avisa quando não encontra", async () => {
    apiDaEntrada();
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();

    await buscar("joao avi");
    expect(await screen.findByRole("button", { name: "João Ávila" })).toBeInTheDocument();

    await buscar("ninguém");
    expect(await screen.findByText(/Nenhum nome encontrado/)).toBeInTheDocument();
  });

  it("lembra a última empresa e escolhe sozinho o único setor", async () => {
    window.localStorage.setItem("uniquiz.empresa", "7");
    apiDaEntrada({}, [
      { id: 1, nome: "Unimed Exemplo" },
      { id: 7, nome: "Unimed Vizinha" },
    ]);
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });

    expect(await screen.findByRole("option", { name: "Unimed Vizinha", selected: true })).toBeInTheDocument();
    expect(await screen.findByRole("option", { name: "Comercial", selected: true })).toBeInTheDocument();
    await buscar("bru");
    expect(await screen.findByRole("button", { name: "Bruno Teste" })).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Empresa"), "Unimed Exemplo");
    expect(window.localStorage.getItem("uniquiz.empresa")).toBe("1");
    expect(screen.queryByLabelText("Seu nome")).not.toBeInTheDocument();
  });

  it("permite voltar da confirmação e mostra o erro do servidor", async () => {
    apiDaEntrada({
      status: 422,
      corpo: { detail: "PIN incorreto. Restam 2 tentativas antes do bloqueio." },
    });
    renderizar(<TelaDeEntrada />, { rota: "/entrar", caminho: "/entrar" });
    await escolherSetor();
    await buscar("bru");

    await userEvent.click(await screen.findByRole("button", { name: "Bruno Teste" }));
    await userEvent.type(screen.getByLabelText("PIN"), "999");
    await userEvent.click(screen.getByRole("button", { name: "Sim, sou eu" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Restam 2 tentativas");

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

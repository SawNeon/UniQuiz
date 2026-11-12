import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { simularApi } from "../testes/utilidades";

const perfil = {
  nome: "Ana Exemplo",
  setor: "TI",
  empresa: "Unimed Exemplo",
  pontosNaTemporada: 1240,
  pontosTotais: 1240,
  nivel: 3,
  pontosParaProximoNivel: 260,
  diasSeguidos: 5,
  maiorSequencia: 5,
  trofeus: [],
  certificados: [],
};

async function abrir(caminho: string) {
  vi.resetModules();
  window.history.pushState({}, "", caminho);
  const { App } = await import("./App");
  render(<App />);
}

describe("App", () => {
  it("abre a tela de entrada sem sessão", async () => {
    simularApi({ "GET /eu": { status: 401 }, "GET /empresas": { corpo: [] } });
    await abrir("/");

    expect(await screen.findByRole("heading", { name: "UniQuiz" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/entrar");
  });

  it("mostra o caminho com pontos, sequência, som e menu para quem tem sessão", async () => {
    simularApi({ "GET /eu": { corpo: perfil }, "GET /caminho": { corpo: [] } });
    await abrir("/");

    expect(await screen.findByText("1.240 pontos")).toBeInTheDocument();
    expect(screen.getByText("5 dias")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Caminho" })).toHaveAttribute("aria-current", "page");

    const som = screen.getByRole("button", { name: "Ligar o som" });
    await userEvent.click(som);
    expect(screen.getByRole("button", { name: "Desligar o som" })).toHaveAttribute("aria-pressed", "true");
    await userEvent.click(screen.getByRole("button", { name: "Desligar o som" }));

    await userEvent.click(screen.getByRole("link", { name: "Perfil" }));
    expect(await screen.findByRole("button", { name: "Não sou eu" })).toBeInTheDocument();
  });

  it("carrega o login do admin sob demanda", async () => {
    simularApi({});
    await abrir("/admin/entrar");

    expect(await screen.findByRole("heading", { name: "UniQuiz · Administração" })).toBeInTheDocument();
  });

  it("mostra uma página amigável para um endereço que não existe", async () => {
    simularApi({});
    await abrir("/qualquer-coisa");

    expect(await screen.findByRole("heading", { name: "Não encontramos essa página" })).toBeInTheDocument();
    expect(screen.getByText("/qualquer-coisa")).toBeInTheDocument();
    expect(screen.queryByText(/Hey developer/i)).not.toBeInTheDocument();
  });

  it("leva o /index.html para o início", async () => {
    simularApi({ "GET /eu": { status: 401 }, "GET /empresas": { corpo: [] } });
    await abrir("/index.html");

    expect(await screen.findByRole("heading", { name: "UniQuiz" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/entrar");
  });

  it("abre a validação pública do certificado sem sessão", async () => {
    simularApi({ "GET /publico/certificados/ABC": { status: 404, corpo: { detail: "Não encontrado." } } });
    await abrir("/certificado/ABC");

    expect(await screen.findByRole("heading", { name: "Validação de certificado" })).toBeInTheDocument();
  });
});

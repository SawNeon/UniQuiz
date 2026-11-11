import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { simularApi } from "../testes/utilidades";
import { ExigeSessao } from "./ExigeSessao";

function renderizarProtegido() {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<ExigeSessao />}>
            <Route path="/" element={<p>Área do colaborador</p>} />
          </Route>
          <Route path="/entrar" element={<p>Tela de entrada</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ExigeSessao", () => {
  it("deixa entrar quem tem sessão", async () => {
    simularApi({ "GET /eu": { corpo: { nome: "Ana" } } });
    renderizarProtegido();

    expect(await screen.findByText("Área do colaborador")).toBeInTheDocument();
  });

  it("manda para a entrada quem não tem sessão", async () => {
    simularApi({ "GET /eu": { status: 401 } });
    renderizarProtegido();

    expect(await screen.findByText("Tela de entrada")).toBeInTheDocument();
  });

  it("mostra o erro quando o servidor falha por outro motivo", async () => {
    simularApi({ "GET /eu": { status: 500, corpo: { detail: "Algo deu errado." } } });
    renderizarProtegido();

    expect(await screen.findByRole("alert")).toHaveTextContent("Algo deu errado.");
  });
});

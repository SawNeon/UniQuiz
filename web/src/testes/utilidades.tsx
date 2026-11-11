import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";

export interface RespostaSimulada {
  status?: number;
  corpo?: unknown;
}

type Rota = RespostaSimulada | ((corpo: unknown) => RespostaSimulada);

export interface Chamada {
  metodo: string;
  caminho: string;
  corpo: unknown;
}

export function simularApi(rotas: Record<string, Rota>): Chamada[] {
  const chamadas: Chamada[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (entrada: RequestInfo | URL, opcoes?: RequestInit) => {
      const metodo = opcoes?.method ?? "GET";
      const caminho = String(entrada).replace(/^\/api/, "");
      const corpo = typeof opcoes?.body === "string" ? (JSON.parse(opcoes.body) as unknown) : opcoes?.body;
      chamadas.push({ metodo, caminho, corpo });

      const rota = rotas[`${metodo} ${caminho}`];
      if (rota === undefined) {
        return new Response(JSON.stringify({ detail: `Rota não simulada: ${metodo} ${caminho}` }), {
          status: 404,
        });
      }
      const resposta = typeof rota === "function" ? rota(corpo) : rota;
      const status = resposta.status ?? (resposta.corpo === undefined ? 204 : 200);
      return new Response(status === 204 ? null : JSON.stringify(resposta.corpo), {
        status,
        headers: { "Content-Type": "application/json" },
      });
    }),
  );
  return chamadas;
}

function LocalAtual() {
  return <p>Página: {useLocation().pathname}</p>;
}

export function renderizar(elemento: ReactElement, { rota = "/", caminho = "/" } = {}) {
  const cliente = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={[rota]}>
        <Routes>
          <Route path={caminho} element={elemento} />
          <Route path="*" element={<LocalAtual />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

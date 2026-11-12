import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { ErroDaRota, TelaDePaginaNaoEncontrada } from "./TelasDeErro";

function Quebrado(): never {
  throw new Error("falha de teste");
}

function abrirComErro(rota: Parameters<typeof createMemoryRouter>[0][number], aoRecarregar = vi.fn()) {
  const roteador = createMemoryRouter(
    [
      { path: "/", element: <p>Tela inicial</p> },
      { ...rota, errorElement: <ErroDaRota aoRecarregar={aoRecarregar} /> },
    ],
    { initialEntries: ["/problema"] },
  );
  render(<RouterProvider router={roteador} />);
  return { roteador, aoRecarregar };
}

describe("telas de erro", () => {
  beforeEach(() => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("explica que a página não existe e mostra o endereço tentado", async () => {
    const roteador = createMemoryRouter(
      [
        { path: "/", element: <p>Tela inicial</p> },
        { path: "*", element: <TelaDePaginaNaoEncontrada /> },
      ],
      { initialEntries: ["/caminho/que-nao-existe"] },
    );
    render(<RouterProvider router={roteador} />);

    expect(screen.getByRole("heading", { name: "Não encontramos essa página" })).toBeInTheDocument();
    expect(screen.getByText("/caminho/que-nao-existe")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Ir para o início" }));
    expect(await screen.findByText("Tela inicial")).toBeInTheDocument();
  });

  it("troca o erro técnico por uma tela amigável com opção de recarregar", async () => {
    const { aoRecarregar } = abrirComErro({ path: "/problema", element: <Quebrado /> });

    expect(await screen.findByRole("heading", { name: "Algo deu errado" })).toBeInTheDocument();
    expect(screen.queryByText(/Hey developer/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/falha de teste/)).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Recarregar" }));
    expect(aoRecarregar).toHaveBeenCalledOnce();
  });

  it("deixa voltar ao início depois de um erro", async () => {
    abrirComErro({ path: "/problema", element: <Quebrado /> });
    await screen.findByRole("heading", { name: "Algo deu errado" });

    await userEvent.click(screen.getByRole("button", { name: "Ir para o início" }));

    expect(await screen.findByText("Tela inicial")).toBeInTheDocument();
  });

  it("trata um 404 vindo da rota como página não encontrada", async () => {
    abrirComErro({
      path: "/problema",
      loader: () => {
        throw new Response("Not Found", { status: 404 });
      },
      element: <p>Nunca aparece</p>,
    });

    expect(await screen.findByRole("heading", { name: "Não encontramos essa página" })).toBeInTheDocument();
  });
});

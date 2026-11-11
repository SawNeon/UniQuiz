import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ModuloNoCaminho } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDoCaminho } from "./TelaDoCaminho";

const perfil = {
  nome: "Ana Exemplo",
  setor: "TI",
  empresa: "Unimed Exemplo",
  pontosNaTemporada: 0,
  pontosTotais: 0,
  nivel: 1,
  pontosParaProximoNivel: 500,
  diasSeguidos: 0,
  maiorSequencia: 0,
  trofeus: [],
  certificados: [],
};

const caminho: ModuloNoCaminho[] = [
  {
    id: 10,
    titulo: "LGPD na prática",
    descricao: "",
    temMaterial: true,
    concluido: true,
    licoesConcluidas: 1,
    licoes: [{ id: 1, titulo: "O básico da LGPD", situacao: "CONCLUIDA" }],
  },
  {
    id: 20,
    titulo: "IA com responsabilidade",
    descricao: "",
    temMaterial: false,
    concluido: false,
    licoesConcluidas: 0,
    licoes: [
      { id: 2, titulo: "IA no trabalho", situacao: "DISPONIVEL" },
      { id: 3, titulo: "Ferramentas aprovadas", situacao: "BLOQUEADA" },
    ],
  },
];

describe("TelaDoCaminho", () => {
  it("mostra cada lição conforme a situação", async () => {
    simularApi({ "GET /caminho": { corpo: caminho }, "GET /eu": { corpo: perfil } });
    renderizar(<TelaDoCaminho />);

    expect(await screen.findByRole("heading", { name: "Módulo 1 · LGPD na prática" })).toBeInTheDocument();
    expect(screen.getByText("1 de 1 lições")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Refazer a lição concluída: O básico da LGPD" })).toHaveAttribute(
      "href",
      "/licoes/1",
    );
    expect(screen.getByRole("link", { name: "Começar a lição: IA no trabalho" })).toHaveAttribute(
      "href",
      "/licoes/2",
    );
    expect(screen.getByRole("img", { name: "Lição bloqueada: Ferramentas aprovadas" })).toBeInTheDocument();
    expect(screen.getByText("Você está aqui")).toBeInTheDocument();
  });

  it("leva ao conteúdo de estudo só quando o módulo tem material", async () => {
    simularApi({ "GET /caminho": { corpo: caminho }, "GET /eu": { corpo: perfil } });
    renderizar(<TelaDoCaminho />);

    const links = await screen.findAllByRole("link", { name: "Estudar o conteúdo" });
    expect(links).toHaveLength(1);
    expect(links[0]).toHaveAttribute("href", "/modulos/10/estudo");
  });

  it("gera o certificado do módulo concluído e oferece o download", async () => {
    const chamadas = simularApi({
      "GET /caminho": { corpo: caminho },
      "GET /eu": { corpo: perfil },
      "POST /modulos/10/certificado": {
        corpo: { codigo: "ABCD2345EFGH", moduloId: 10, modulo: "LGPD na prática", emitidoEm: "2026-11-10" },
      },
    });
    renderizar(<TelaDoCaminho />);

    await userEvent.click(await screen.findByRole("button", { name: "Gerar certificado" }));

    expect(await screen.findByRole("link", { name: "Baixar certificado" })).toHaveAttribute(
      "href",
      "/api/certificados/ABCD2345EFGH/pdf",
    );
    expect(chamadas.map((chamada) => `${chamada.metodo} ${chamada.caminho}`)).toContain(
      "POST /modulos/10/certificado",
    );
  });

  it("mostra o certificado já emitido sem gerar de novo", async () => {
    simularApi({
      "GET /caminho": { corpo: caminho },
      "GET /eu": {
        corpo: {
          ...perfil,
          certificados: [
            { codigo: "JA0EMITIDO22", moduloId: 10, modulo: "LGPD na prática", emitidoEm: "2026-11-01" },
          ],
        },
      },
    });
    renderizar(<TelaDoCaminho />);

    expect(await screen.findByRole("link", { name: "Baixar certificado" })).toHaveAttribute(
      "href",
      "/api/certificados/JA0EMITIDO22/pdf",
    );
  });

  it("avisa quando ainda não há desafios publicados", async () => {
    simularApi({ "GET /caminho": { corpo: [] } });
    renderizar(<TelaDoCaminho />);

    expect(await screen.findByText(/A TI está preparando os primeiros desafios/)).toBeInTheDocument();
  });

  it("mostra o erro e permite tentar de novo", async () => {
    simularApi({
      "GET /caminho": { status: 500, corpo: { detail: "Algo deu errado. Tente de novo em instantes." } },
    });
    renderizar(<TelaDoCaminho />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Algo deu errado");
    expect(screen.getByRole("button", { name: "Tentar de novo" })).toBeInTheDocument();
  });
});

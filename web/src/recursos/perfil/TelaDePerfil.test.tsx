import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { Perfil } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { TelaDePerfil } from "./TelaDePerfil";

const perfil: Perfil = {
  nome: "Ana Exemplo",
  setor: "Tecnologia da Informação",
  empresa: "Unimed Exemplo",
  pontosNaTemporada: 1240,
  pontosTotais: 1700,
  nivel: 4,
  pontosParaProximoNivel: 300,
  diasSeguidos: 5,
  maiorSequencia: 8,
  trofeus: [
    {
      codigo: "PRIMEIRO_PASSO",
      nome: "Primeiro passo",
      descricao: "Concluiu a primeira lição.",
      icone: "passo",
      conquistadoEm: "2026-11-10T12:00:00Z",
    },
    {
      codigo: "SEQUENCIA_7",
      nome: "Sequência de 7 dias",
      descricao: "Estudou 7 dias seguidos.",
      icone: "chama",
    },
  ],
  certificados: [{ codigo: "ABCD2345EFGH", moduloId: 1, modulo: "LGPD na prática", emitidoEm: "2026-12-18" }],
};

describe("TelaDePerfil", () => {
  it("mostra nível, pontos, sequência, troféus e certificados", async () => {
    simularApi({ "GET /eu": { corpo: perfil } });
    renderizar(<TelaDePerfil />);

    expect(await screen.findByRole("heading", { name: "Ana Exemplo" })).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("Faltam 300 pontos para o próximo")).toBeInTheDocument();
    expect(screen.getByText("1.240")).toBeInTheDocument();
    expect(screen.getByText("5 agora · maior: 8")).toBeInTheDocument();
    expect(screen.getByText("Conquistado")).toBeInTheDocument();
    expect(screen.getByText("Ainda não conquistado")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar o certificado de LGPD na prática" })).toHaveAttribute(
      "href",
      "/api/certificados/ABCD2345EFGH/pdf",
    );
    expect(screen.getByText("Emitido em 18/12/2026")).toBeInTheDocument();
  });

  it("explica como ganhar o primeiro certificado", async () => {
    simularApi({ "GET /eu": { corpo: { ...perfil, certificados: [] } } });
    renderizar(<TelaDePerfil />);

    expect(await screen.findByText(/Conclua todas as lições de um módulo/)).toBeInTheDocument();
  });

  it("não sou eu encerra a sessão e volta para a entrada", async () => {
    const chamadas = simularApi({ "GET /eu": { corpo: perfil }, "DELETE /sessao": {} });
    renderizar(<TelaDePerfil />);

    await userEvent.click(await screen.findByRole("button", { name: "Não sou eu" }));

    expect(await screen.findByText("Página: /entrar")).toBeInTheDocument();
    expect(chamadas).toContainEqual({ metodo: "DELETE", caminho: "/sessao", corpo: undefined });
  });
});

import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { AlertasDeAuditoria, ContaLogada, PaginaDeAuditoria } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { ContextoDaConta } from "./painel";
import { TelaDeAuditoria } from "./TelaDeAuditoria";

const auditor: ContaLogada = {
  id: 2,
  nome: "Rui Auditor",
  usuario: "rui",
  perfil: "AUDITOR",
  permissoes: ["VER_RELATORIOS", "EXPORTAR_RELATORIOS", "VER_AUDITORIA"],
  precisaTrocarSenha: false,
};

const administrador: ContaLogada = {
  ...auditor,
  id: 1,
  perfil: "ADMINISTRADOR",
  permissoes: [...auditor.permissoes, "GERIR_USUARIOS_DO_PAINEL"],
};

const semAlertas: AlertasDeAuditoria = {
  desde: "2026-11-12T12:00:00Z",
  falhasPorPessoa: [],
  falhasPorIp: [],
  contasBloqueadas: [],
};

const comAlertas: AlertasDeAuditoria = {
  desde: "2026-11-12T12:00:00Z",
  falhasPorPessoa: [{ quem: "Gil Gestor", falhas: 4, ultima: "2026-11-13T11:00:00Z" }],
  falhasPorIp: [{ ip: "10.0.0.9", falhas: 7, ultima: "2026-11-13T11:05:00Z" }],
  contasBloqueadas: [
    { tipo: "ADMINISTRADOR", id: 3, nome: "Gil Gestor", ate: "2026-11-13T11:20:00Z" },
    { tipo: "COLABORADOR", id: 8, nome: "Ana Exemplo", ate: "2026-11-13T11:25:00Z" },
  ],
};

function pagina(total: number, pagina = 0): PaginaDeAuditoria {
  return {
    pagina,
    tamanho: 50,
    total,
    itens: [
      {
        id: 10 + pagina,
        momento: "2026-11-13T11:00:00Z",
        atorTipo: "ANONIMO",
        atorNome: "fulano",
        acao: "ENTRADA_NO_PAINEL",
        acaoDescricao: "Entrou no painel",
        resultado: "FALHA",
        detalhe: "Usuário inexistente ou desativado",
        ip: "10.0.0.9",
      },
      {
        id: 20 + pagina,
        momento: "2026-11-13T10:00:00Z",
        atorTipo: "ADMINISTRADOR",
        atorId: 1,
        atorNome: "Ana Admin",
        acao: "USUARIO_DO_PAINEL_CRIADO",
        acaoDescricao: "Criou um usuário do painel",
        alvo: "usuarioId=3",
        resultado: "SUCESSO",
      },
    ],
  };
}

const acoes = [
  { codigo: "ENTRADA_NO_PAINEL", descricao: "Entrou no painel" },
  { codigo: "SENHA_TROCADA", descricao: "Trocou a própria senha" },
];

describe("TelaDeAuditoria", () => {
  it("mostra o registro e avisa quando não há alertas", async () => {
    simularApi({
      "GET /admin/auditoria?pagina=0": { corpo: pagina(2) },
      "GET /admin/auditoria/alertas": { corpo: semAlertas },
      "GET /admin/auditoria/acoes": { corpo: acoes },
    });
    renderizar(
      <ContextoDaConta value={auditor}>
        <TelaDeAuditoria />
      </ContextoDaConta>,
    );

    expect(await screen.findByText(/Nenhum alerta/)).toBeInTheDocument();
    expect(await screen.findByText("2 registros")).toBeInTheDocument();
    const [, primeira, segunda] = screen.getAllByRole("row");
    if (!primeira || !segunda) {
      throw new Error("A tabela deveria ter duas linhas de registro.");
    }
    expect(within(primeira).getByText("fulano")).toBeInTheDocument();
    expect(within(primeira).getByText("Falhou")).toBeInTheDocument();
    expect(within(primeira).getByText("Usuário inexistente ou desativado")).toBeInTheDocument();
    expect(within(segunda).getByText("usuarioId=3")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Páginas do registro" })).not.toBeInTheDocument();
  });

  it("filtra, limpa e pagina", async () => {
    const chamadas = simularApi({
      "GET /admin/auditoria?pagina=0": { corpo: pagina(120) },
      "GET /admin/auditoria?pagina=1": { corpo: pagina(120, 1) },
      "GET /admin/auditoria?pagina=0&acao=ENTRADA_NO_PAINEL&resultado=FALHA&busca=fulano&de=2026-11-01": {
        corpo: pagina(1),
      },
      "GET /admin/auditoria/alertas": { corpo: semAlertas },
      "GET /admin/auditoria/acoes": { corpo: acoes },
    });
    renderizar(
      <ContextoDaConta value={auditor}>
        <TelaDeAuditoria />
      </ContextoDaConta>,
    );

    expect(await screen.findByText("Página 1 de 3")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByText("Página 2 de 3")).toBeInTheDocument();

    await userEvent.selectOptions(await screen.findByLabelText("Ação"), "ENTRADA_NO_PAINEL");
    await userEvent.selectOptions(screen.getByLabelText("Resultado"), "FALHA");
    await userEvent.type(screen.getByLabelText("Nome, alvo ou IP"), "  fulano ");
    await userEvent.type(screen.getByLabelText("De"), "2026-11-01");
    await userEvent.click(screen.getByRole("button", { name: "Filtrar" }));
    expect(await screen.findByText("1 registro")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Limpar filtros" }));
    expect(await screen.findByText("120 registros")).toBeInTheDocument();
    expect(screen.getByLabelText("Resultado")).toHaveValue("");
    expect(chamadas.some((chamada) => chamada.caminho.includes("busca=fulano&"))).toBe(true);
  });

  it("mostra os alertas e deixa o Administrador desbloquear a conta do painel", async () => {
    const chamadas = simularApi({
      "GET /admin/auditoria?pagina=0": { corpo: pagina(0) },
      "GET /admin/auditoria/alertas": { corpo: comAlertas },
      "GET /admin/auditoria/acoes": { corpo: acoes },
      "POST /admin/usuarios/3/desbloquear": {},
    });
    renderizar(
      <ContextoDaConta value={administrador}>
        <TelaDeAuditoria />
      </ContextoDaConta>,
    );

    expect(await screen.findByText("10.0.0.9")).toBeInTheDocument();
    expect(screen.getByText(/4 falhas/)).toBeInTheDocument();
    expect(screen.getByText("Ana Exemplo")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Desbloquear Ana Exemplo" })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Desbloquear Gil Gestor" }));
    expect(chamadas.some((chamada) => chamada.caminho === "/admin/usuarios/3/desbloquear")).toBe(true);
  });

  it("não mostra o desbloqueio para quem não gere usuários", async () => {
    simularApi({
      "GET /admin/auditoria?pagina=0": { corpo: { ...pagina(0), itens: [] } },
      "GET /admin/auditoria/alertas": { corpo: comAlertas },
      "GET /admin/auditoria/acoes": { corpo: acoes },
    });
    renderizar(
      <ContextoDaConta value={auditor}>
        <TelaDeAuditoria />
      </ContextoDaConta>,
    );

    expect(await screen.findByText("Nenhum registro com esses filtros.")).toBeInTheDocument();
    expect(await screen.findByText("10.0.0.9")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Desbloquear/ })).not.toBeInTheDocument();
  });
});

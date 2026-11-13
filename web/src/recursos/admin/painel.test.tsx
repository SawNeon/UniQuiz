import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactElement } from "react";
import { Route, Routes } from "react-router";
import type { ContaLogada, SessaoAtiva, UsuarioDoPainel } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { ExigePermissao } from "./ExigePermissao";
import { InicioDoPainel } from "./InicioDoPainel";
import { LayoutAdmin } from "./LayoutAdmin";
import { ContextoDaConta } from "./painel";
import { TelaDeMinhaConta } from "./TelaDeMinhaConta";
import { TelaDeUsuarios } from "./TelaDeUsuarios";

const administrador: ContaLogada = {
  id: 1,
  nome: "Ana Admin",
  usuario: "ana",
  perfil: "ADMINISTRADOR",
  permissoes: [
    "GERIR_CONTEUDO",
    "GERIR_COLABORADORES",
    "GERIR_TEMPORADAS",
    "GERIR_USUARIOS_DO_PAINEL",
    "VER_RELATORIOS",
    "EXPORTAR_RELATORIOS",
    "VER_AUDITORIA",
  ],
  precisaTrocarSenha: false,
};

const auditor: ContaLogada = {
  id: 2,
  nome: "Rui Auditor",
  usuario: "rui",
  perfil: "AUDITOR",
  permissoes: ["VER_RELATORIOS", "EXPORTAR_RELATORIOS", "VER_AUDITORIA"],
  precisaTrocarSenha: false,
};

const usuarios: UsuarioDoPainel[] = [
  {
    id: 1,
    nome: "Ana Admin",
    usuario: "ana",
    perfil: "ADMINISTRADOR",
    setores: [],
    ativo: true,
    bloqueado: false,
    precisaTrocarSenha: false,
    criadoEm: "2026-11-01T12:00:00Z",
    ultimoAcessoEm: "2026-11-13T12:00:00Z",
  },
  {
    id: 3,
    nome: "Gil Gestor",
    usuario: "gil",
    perfil: "GESTOR",
    setores: [10],
    ativo: true,
    bloqueado: true,
    precisaTrocarSenha: true,
    criadoEm: "2026-11-01T12:00:00Z",
    ultimoAcessoEm: null,
  },
];

const setores = [
  { id: 10, nome: "Unimed Exemplo · Atendimento" },
  { id: 11, nome: "Unimed Exemplo · Financeiro" },
];

function comConta(conta: ContaLogada, elemento: ReactElement) {
  return <ContextoDaConta value={conta}>{elemento}</ContextoDaConta>;
}

describe("LayoutAdmin por perfil", () => {
  it("mostra ao auditor só o que ele pode abrir", async () => {
    simularApi({ "GET /admin/eu": { corpo: auditor } });
    renderizar(<LayoutAdmin />, { rota: "/admin", caminho: "/admin" });

    expect(await screen.findByRole("link", { name: "Minha conta" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Conteúdo" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Usuários do painel" })).not.toBeInTheDocument();
    expect(screen.getByText("Rui Auditor · Auditor")).toBeInTheDocument();
  });

  it("obriga a trocar a senha provisória antes de qualquer coisa", async () => {
    const chamadas = simularApi({
      "GET /admin/eu": { corpo: { ...administrador, permissoes: [], precisaTrocarSenha: true } },
      "POST /admin/minha-conta/senha": {},
    });
    renderizar(<LayoutAdmin />, { rota: "/admin", caminho: "/admin" });

    expect(await screen.findByRole("heading", { name: "Crie a sua senha" })).toBeInTheDocument();
    expect(screen.queryByRole("navigation")).not.toBeInTheDocument();

    await userEvent.type(screen.getByLabelText("Senha provisória"), "Abcd-Efgh-Jkmn-Pqrs");
    await userEvent.type(screen.getByLabelText("Nova senha"), "cafe forte antes da reuniao");
    await userEvent.type(screen.getByLabelText("Repita a nova senha"), "cafe forte antes da reuniao");
    await userEvent.click(screen.getByRole("button", { name: "Trocar senha" }));

    expect(await screen.findByText(/Senha trocada/)).toBeInTheDocument();
    expect(chamadas.find((chamada) => chamada.metodo === "POST")?.corpo).toEqual({
      senhaAtual: "Abcd-Efgh-Jkmn-Pqrs",
      novaSenha: "cafe forte antes da reuniao",
    });
  });
});

describe("InicioDoPainel e ExigePermissao", () => {
  it("leva quem não gere conteúdo para a primeira seção permitida", async () => {
    simularApi({});
    renderizar(comConta(auditor, <InicioDoPainel />), { rota: "/admin", caminho: "/admin" });

    expect(await screen.findByText("Página: /admin/auditoria")).toBeInTheDocument();
  });

  it("explica quando o perfil não abre a área", () => {
    renderizar(
      comConta(
        auditor,
        <Routes>
          <Route
            path="*"
            element={
              <ExigePermissao permissao="GERIR_USUARIOS_DO_PAINEL">
                <p>Conteúdo restrito</p>
              </ExigePermissao>
            }
          />
        </Routes>,
      ),
    );

    expect(screen.getByRole("heading", { name: "Sem acesso a esta área" })).toBeInTheDocument();
    expect(screen.queryByText("Conteúdo restrito")).not.toBeInTheDocument();
  });

  it("mostra a área quando o perfil permite", () => {
    renderizar(
      comConta(
        administrador,
        <ExigePermissao permissao="GERIR_USUARIOS_DO_PAINEL">
          <p>Conteúdo restrito</p>
        </ExigePermissao>,
      ),
    );

    expect(screen.getByText("Conteúdo restrito")).toBeInTheDocument();
  });
});

describe("TelaDeMinhaConta", () => {
  const sessoes: SessaoAtiva[] = [
    {
      id: "aaaa1111bbbb2222",
      criadaEm: "2026-11-13T12:00:00Z",
      ultimoUsoEm: "2026-11-13T12:10:00Z",
      ip: "10.0.0.5",
      dispositivo: "Chrome no Windows",
      atual: true,
    },
    {
      id: "cccc3333dddd4444",
      criadaEm: "2026-11-12T09:00:00Z",
      ultimoUsoEm: "2026-11-12T09:30:00Z",
      dispositivo: "Safari no iPhone",
      atual: false,
    },
  ];

  it("mostra os dados, a força da senha e confere a nova senha antes de enviar", async () => {
    const chamadas = simularApi({ "GET /admin/minha-conta/sessoes": { corpo: [] } });
    renderizar(comConta(auditor, <TelaDeMinhaConta />));

    expect(screen.getByText("rui")).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Senha atual"), "atual");
    await userEvent.type(screen.getByLabelText("Nova senha"), "curta");
    expect(screen.getByText("Força da senha: Curta demais")).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Repita a nova senha"), "outra");
    await userEvent.click(screen.getByRole("button", { name: "Trocar senha" }));

    expect(await screen.findByText("Use pelo menos 12 caracteres.")).toBeInTheDocument();
    expect(screen.getByText("As duas senhas precisam ser iguais.")).toBeInTheDocument();
    expect(chamadas.filter((chamada) => chamada.metodo === "POST")).toHaveLength(0);
  });

  it("lista as sessões e encerra as de outros aparelhos", async () => {
    const chamadas = simularApi({
      "GET /admin/minha-conta/sessoes": { corpo: sessoes },
      "DELETE /admin/minha-conta/sessoes/cccc3333dddd4444": {},
      "POST /admin/minha-conta/sessoes/encerrar-outras": {},
    });
    renderizar(comConta(auditor, <TelaDeMinhaConta />));

    const daqui = await screen.findByRole("listitem", { name: "Chrome no Windows" });
    expect(within(daqui).getByText("Este aparelho")).toBeInTheDocument();
    expect(within(daqui).queryByRole("button", { name: "Encerrar" })).not.toBeInTheDocument();
    expect(within(daqui).getByText(/IP 10\.0\.0\.5/)).toBeInTheDocument();

    const iphone = screen.getByRole("listitem", { name: "Safari no iPhone" });
    await userEvent.click(within(iphone).getByRole("button", { name: "Encerrar" }));
    await userEvent.click(screen.getByRole("button", { name: "Sair dos outros aparelhos" }));

    expect(chamadas.map((chamada) => `${chamada.metodo} ${chamada.caminho}`)).toEqual(
      expect.arrayContaining([
        "DELETE /admin/minha-conta/sessoes/cccc3333dddd4444",
        "POST /admin/minha-conta/sessoes/encerrar-outras",
      ]),
    );
  });

  it("mostra a recusa do servidor", async () => {
    simularApi({
      "GET /admin/minha-conta/sessoes": { corpo: [] },
      "POST /admin/minha-conta/senha": { status: 422, corpo: { detail: "A senha atual não confere." } },
    });
    renderizar(comConta(auditor, <TelaDeMinhaConta />));

    await userEvent.type(screen.getByLabelText("Senha atual"), "errada-errada");
    await userEvent.type(screen.getByLabelText("Nova senha"), "uma frase longa qualquer");
    await userEvent.type(screen.getByLabelText("Repita a nova senha"), "uma frase longa qualquer");
    await userEvent.click(screen.getByRole("button", { name: "Trocar senha" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("A senha atual não confere.");
  });
});

describe("TelaDeUsuarios", () => {
  function rotasDaTela(extras = {}) {
    return simularApi({
      "GET /admin/usuarios": { corpo: usuarios },
      "GET /admin/usuarios/setores": { corpo: setores },
      ...extras,
    });
  }

  it("lista com perfil, setores e situação, sem ações contra si mesmo", async () => {
    rotasDaTela();
    renderizar(comConta(administrador, <TelaDeUsuarios />));

    const gil = await screen.findByRole("listitem", { name: "Gil Gestor" });
    expect(within(gil).getByText(/gil · Gestor · Unimed Exemplo · Atendimento/)).toBeInTheDocument();
    expect(within(gil).getByText("Bloqueado")).toBeInTheDocument();
    expect(within(gil).getByText("Senha provisória")).toBeInTheDocument();
    expect(within(gil).getByText("Ainda não entrou")).toBeInTheDocument();

    const ana = screen.getByRole("listitem", { name: "Ana Admin" });
    expect(within(ana).getByText("Ana Admin (você)")).toBeInTheDocument();
    expect(within(ana).queryByRole("button", { name: "Desativar" })).not.toBeInTheDocument();
  });

  it("cria um gestor e mostra a senha provisória uma única vez", async () => {
    const chamadas = rotasDaTela({
      "POST /admin/usuarios": {
        status: 201,
        corpo: { usuarioId: 9, usuario: "maria.silva", senha: "Abcd-Efgh-Jkmn-Pqrs" },
      },
    });
    renderizar(comConta(administrador, <TelaDeUsuarios />));

    await userEvent.click(await screen.findByRole("button", { name: "Novo usuário" }));
    await userEvent.click(screen.getByRole("button", { name: "Criar usuário" }));
    expect(await screen.findByText("Escolha ao menos um setor para o Gestor.")).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText("Nome"), "Maria Silva");
    await userEvent.type(screen.getByLabelText("Login"), "Maria.Silva");
    await userEvent.click(screen.getByLabelText("Unimed Exemplo · Financeiro"));
    await userEvent.click(screen.getByRole("button", { name: "Criar usuário" }));

    expect(await screen.findByLabelText("Senha provisória")).toHaveTextContent("Abcd-Efgh-Jkmn-Pqrs");
    expect(chamadas.find((chamada) => chamada.metodo === "POST")?.corpo).toEqual({
      nome: "Maria Silva",
      usuario: "maria.silva",
      perfil: "GESTOR",
      setores: [11],
    });

    await userEvent.click(screen.getByRole("button", { name: "Já anotei, fechar" }));
    expect(screen.queryByText("Abcd-Efgh-Jkmn-Pqrs")).not.toBeInTheDocument();
  });

  it("copia a senha provisória", async () => {
    const escrever = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText: escrever }, configurable: true });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    rotasDaTela({
      "POST /admin/usuarios/3/redefinir-senha": {
        corpo: { usuarioId: 3, usuario: "gil", senha: "Wxyz-Efgh-Jkmn-Pqrs" },
      },
    });
    renderizar(comConta(administrador, <TelaDeUsuarios />));

    const gil = await screen.findByRole("listitem", { name: "Gil Gestor" });
    await userEvent.click(within(gil).getByRole("button", { name: "Redefinir senha" }));
    await userEvent.click(await screen.findByRole("button", { name: "Copiar senha" }));

    expect(escrever).toHaveBeenCalledWith("Wxyz-Efgh-Jkmn-Pqrs");
    expect(await screen.findByRole("button", { name: "Copiada!" })).toBeInTheDocument();
  });

  it("desbloqueia, encerra sessões e desativa só depois de confirmar", async () => {
    const confirmar = vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValue(true);
    const chamadas = rotasDaTela({
      "POST /admin/usuarios/3/desbloquear": {},
      "POST /admin/usuarios/3/encerrar-sessoes": {},
      "POST /admin/usuarios/3/desativar": {},
    });
    renderizar(comConta(administrador, <TelaDeUsuarios />));

    const gil = await screen.findByRole("listitem", { name: "Gil Gestor" });
    await userEvent.click(within(gil).getByRole("button", { name: "Desativar" }));
    await userEvent.click(within(gil).getByRole("button", { name: "Desativar" }));
    await userEvent.click(within(gil).getByRole("button", { name: "Desbloquear" }));
    await userEvent.click(within(gil).getByRole("button", { name: "Encerrar sessões" }));

    const acoes = chamadas.filter((chamada) => chamada.metodo === "POST").map((chamada) => chamada.caminho);
    expect(acoes).toEqual([
      "/admin/usuarios/3/desativar",
      "/admin/usuarios/3/desbloquear",
      "/admin/usuarios/3/encerrar-sessoes",
    ]);
    expect(confirmar).toHaveBeenCalledTimes(3);
  });

  it("edita o perfil sem mudar o login e mostra a recusa do servidor", async () => {
    const chamadas = rotasDaTela({
      "PUT /admin/usuarios/3": {
        status: 422,
        corpo: { detail: "É preciso manter ao menos um Administrador ativo." },
      },
    });
    renderizar(comConta(administrador, <TelaDeUsuarios />));

    const gil = await screen.findByRole("listitem", { name: "Gil Gestor" });
    await userEvent.click(within(gil).getByRole("button", { name: "Editar" }));
    const formulario = screen.getByRole("form", { name: "Editar Gil Gestor" });
    expect(within(formulario).getByLabelText("Login")).toHaveAttribute("readonly");
    await userEvent.click(within(formulario).getByLabelText(/Auditor/));
    expect(within(formulario).queryByLabelText("Unimed Exemplo · Atendimento")).not.toBeInTheDocument();
    await userEvent.click(within(formulario).getByRole("button", { name: "Salvar alterações" }));

    expect(await within(formulario).findByRole("alert")).toHaveTextContent(
      "É preciso manter ao menos um Administrador ativo.",
    );
    expect(chamadas.find((chamada) => chamada.metodo === "PUT")?.corpo).toEqual({
      nome: "Gil Gestor",
      perfil: "AUDITOR",
      setores: [],
    });
  });
});

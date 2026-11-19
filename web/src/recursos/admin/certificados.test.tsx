import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { enderecoDaArte, enderecoDoZip } from "../../api/certificados";
import type {
  ContaLogada,
  FiltrosDosRelatorios,
  ModeloDoCertificado,
  ModuloComCertificado,
  PaginaDeCertificados,
  PosicaoDoCampo,
} from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { ContextoDaConta } from "./painel";
import { TelaDeCertificados } from "./TelaDeCertificados";

const administrador: ContaLogada = {
  id: 1,
  nome: "Admin",
  usuario: "admin",
  perfil: "ADMINISTRADOR",
  permissoes: [
    "GERIR_CONTEUDO",
    "GERIR_COLABORADORES",
    "GERIR_TEMPORADAS",
    "GERIR_USUARIOS_DO_PAINEL",
    "GERIR_CERTIFICADOS",
    "VER_RELATORIOS",
    "EXPORTAR_RELATORIOS",
    "VER_AUDITORIA",
  ],
  precisaTrocarSenha: false,
};

const gestor: ContaLogada = {
  ...administrador,
  id: 3,
  perfil: "GESTOR",
  permissoes: ["VER_RELATORIOS", "EXPORTAR_RELATORIOS"],
};

const filtros: FiltrosDosRelatorios = {
  setores: [{ id: 5, nome: "TI", empresa: "Unimed Exemplo" }],
  modulos: [{ id: 1, titulo: "LGPD" }],
};

function pagina(total: number, numero = 0): PaginaDeCertificados {
  return {
    itens: [
      {
        codigo: `ABCD2345EFG${numero}`,
        codigoFormatado: `ABCD-2345-EFG${numero}`,
        moduloId: 1,
        modulo: "LGPD",
        nome: numero === 0 ? "Ana Exemplo" : "Bruno Teste",
        empresa: "Unimed Exemplo",
        setor: "TI",
        emitidoEm: "2026-11-10",
      },
    ],
    pagina: numero,
    tamanho: 50,
    total,
  };
}

const campos: PosicaoDoCampo[] = [
  posicao("NOME", 50, 41.2, 30, "#00995D", "CENTRO", true, 85, ""),
  posicao("MODULO", 50, 55.4, 24, "#004E4C", "CENTRO", true, 85, ""),
  posicao("DATA", 50, 68.9, 14, "#004E4C", "CENTRO", false, 60, ""),
  posicao("CODIGO", 5.7, 89.9, 12, "#004E4C", "ESQUERDA", true, 50, "Código de validação: "),
  posicao("ENDERECO_DE_VALIDACAO", 5.7, 92.9, 11, "#004E4C", "ESQUERDA", false, 70, "Confira em "),
];

const modelo: ModeloDoCertificado = { campos, temArte: false, avisos: [] };

const modulos: ModuloComCertificado[] = [
  { id: 1, titulo: "LGPD", status: "PUBLICADO", liberado: true, emitidos: 2 },
  { id: 2, titulo: "Golpes", status: "RASCUNHO", liberado: false, emitidos: 1 },
];

function posicao(
  campo: PosicaoDoCampo["campo"],
  x: number,
  y: number,
  tamanho: number,
  cor: string,
  alinhamento: PosicaoDoCampo["alinhamento"],
  negrito: boolean,
  larguraMaxima: number,
  prefixo: string,
): PosicaoDoCampo {
  return { campo, visivel: true, x, y, tamanho, cor, alinhamento, negrito, larguraMaxima, prefixo };
}

function abrir(conta: ContaLogada = administrador) {
  return renderizar(
    <ContextoDaConta.Provider value={conta}>
      <TelaDeCertificados />
    </ContextoDaConta.Provider>,
  );
}

function camposEnviados(corpo: unknown): PosicaoDoCampo[] {
  return (corpo as { campos: PosicaoDoCampo[] }).campos;
}

describe("TelaDeCertificados", () => {
  it("lista os emitidos, filtra, pagina e oferece o ZIP com os mesmos filtros", async () => {
    const chamadas = simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(51) },
      "GET /admin/certificados?pagina=1": { corpo: pagina(51, 1) },
      "GET /admin/certificados?pagina=0&moduloId=1": { corpo: pagina(51) },
      "GET /admin/certificados?pagina=0&moduloId=1&busca=ana": { corpo: pagina(1) },
    });
    abrir();

    expect(await screen.findByText("51 certificados emitidos")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "ABCD-2345-EFG0" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar todos em ZIP (51)" })).toHaveAttribute(
      "href",
      "/api/admin/certificados/exportar",
    );

    await userEvent.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByText("Bruno Teste")).toBeInTheDocument();
    expect(screen.getByText("Página 2 de 2")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Anterior" }));
    expect(await screen.findByText("Ana Exemplo")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Módulo"), "1");
    await userEvent.type(screen.getByLabelText("Nome ou código"), " ana ");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(await screen.findByText("1 certificado emitido")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar todos em ZIP (1)" })).toHaveAttribute(
      "href",
      "/api/admin/certificados/exportar?moduloId=1&busca=ana",
    );
    expect(chamadas.map((chamada) => chamada.caminho)).toContain(
      "/admin/certificados?pagina=0&moduloId=1&busca=ana",
    );
  });

  it("gestor só vê a lista, filtra por setor e sem exportar não ganha o ZIP", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(0) },
      "GET /admin/certificados?pagina=0&setorId=5": { corpo: { ...pagina(0), itens: [] } },
    });
    abrir({ ...gestor, permissoes: ["VER_RELATORIOS"] });

    expect(await screen.findByText("0 certificados emitidos")).toBeInTheDocument();
    expect(screen.queryByRole("tab")).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /ZIP/ })).not.toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Setor"), "5");
    expect(await screen.findByText("Nenhum certificado emitido com esses filtros.")).toBeInTheDocument();
  });

  it("mostra o erro da lista", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { status: 500, corpo: { detail: "Falhou a lista." } },
    });
    abrir(gestor);

    expect(await screen.findByText("Falhou a lista.")).toBeInTheDocument();
  });

  it("libera e trava o certificado de cada módulo", async () => {
    const chamadas = simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(1) },
      "GET /admin/certificados/modulos": { corpo: modulos },
      "POST /admin/certificados/modulos/2/liberacao": {},
      "POST /admin/certificados/modulos/1/liberacao": { status: 422, corpo: { detail: "Não deu." } },
    });
    abrir();

    await userEvent.click(await screen.findByRole("tab", { name: "Liberação" }));
    expect(await screen.findByText(/2 certificados garantidos/)).toBeInTheDocument();
    expect(screen.getByText(/1 certificado garantido/)).toBeInTheDocument();
    expect(screen.getByText("Travado")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Liberar Golpes" }));
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/admin/certificados/modulos/2/liberacao",
      corpo: { liberado: true },
    });

    await userEvent.click(screen.getByRole("button", { name: "Travar LGPD" }));
    expect(await screen.findByText("Não deu.")).toBeInTheDocument();
  });

  it("avisa quando não há módulo e quando a lista falha", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(1) },
      "GET /admin/certificados/modulos": { corpo: [] },
    });
    abrir();

    await userEvent.click(await screen.findByRole("tab", { name: "Liberação" }));
    expect(await screen.findByText("Nenhum módulo criado ainda.")).toBeInTheDocument();
  });

  it("mostra o erro dos módulos", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(1) },
      "GET /admin/certificados/modulos": { status: 500, corpo: { detail: "Sem módulos." } },
    });
    abrir();

    await userEvent.click(await screen.findByRole("tab", { name: "Liberação" }));
    expect(await screen.findByText("Sem módulos.")).toBeInTheDocument();
  });
});

describe("Editor do modelo do certificado", () => {
  async function abrirModelo(rotas: Parameters<typeof simularApi>[0] = {}) {
    const chamadas = simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(1) },
      "GET /admin/certificados/modelo": { corpo: modelo },
      ...rotas,
    });
    abrir();
    await userEvent.click(await screen.findByRole("tab", { name: "Modelo" }));
    await screen.findByRole("group", { name: "Prévia do certificado" });
    return chamadas;
  }

  it("ajusta o campo pelo painel e salva as posições", async () => {
    const chamadas = await abrirModelo({
      "PUT /admin/certificados/modelo": (corpo) => ({ corpo: { ...modelo, campos: camposEnviados(corpo) } }),
    });

    expect(
      screen.getByText("Sem arte enviada: o certificado usa o fundo padrão do UniQuiz."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Salvar posições" })).toBeDisabled();

    await userEvent.selectOptions(screen.getByLabelText("Campo"), "DATA");
    await userEvent.type(screen.getByLabelText("Texto antes do campo"), "Varginha, ");
    fireEvent.change(screen.getByLabelText("Horizontal (%)"), { target: { value: "120" } });
    fireEvent.change(screen.getByLabelText("Vertical (%)"), { target: { value: "70.04" } });
    fireEvent.change(screen.getByLabelText("Tamanho da letra"), { target: { value: "18" } });
    fireEvent.change(screen.getByLabelText("Largura máxima (%)"), { target: { value: "" } });
    await userEvent.click(screen.getByRole("button", { name: "Direita" }));
    await userEvent.click(screen.getByLabelText("Negrito"));
    await userEvent.click(screen.getByRole("button", { name: "Verde-limão" }));
    fireEvent.change(screen.getByLabelText("Outra cor"), { target: { value: "#123abc" } });

    expect(screen.getByText("Há mudanças que ainda não foram salvas.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Data de emissão/ })).toHaveTextContent(/^Varginha, /);

    await userEvent.click(screen.getByRole("button", { name: "Salvar posições" }));

    expect(await screen.findByText(/Posições salvas/)).toBeInTheDocument();
    const enviado = camposEnviados(chamadas.find((chamada) => chamada.metodo === "PUT")?.corpo);
    expect(enviado.find((item) => item.campo === "DATA")).toEqual({
      campo: "DATA",
      visivel: true,
      x: 100,
      y: 70,
      tamanho: 18,
      cor: "#123ABC",
      alinhamento: "DIREITA",
      negrito: true,
      larguraMaxima: 5,
      prefixo: "Varginha, ",
    });
  });

  it("move o campo arrastando e com as setas e esconde campo", async () => {
    await abrirModelo();
    const pagina = screen.getByRole("group", { name: "Prévia do certificado" });
    vi.spyOn(pagina, "getBoundingClientRect").mockReturnValue({
      left: 0,
      top: 0,
      width: 1000,
      height: 700,
      right: 1000,
      bottom: 700,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    });
    const nome = screen.getByRole("button", { name: "Nome da pessoa: arraste para mover" });

    fireEvent.pointerDown(nome, { clientX: 500, clientY: 280, pointerId: 1 });
    fireEvent.pointerMove(nome, { clientX: 300, clientY: 140, pointerId: 1 });
    fireEvent.pointerUp(nome, { pointerId: 1 });
    fireEvent.pointerMove(nome, { clientX: 900, clientY: 600, pointerId: 1 });

    expect(screen.getByLabelText("Horizontal (%)")).toHaveValue(30);
    expect(screen.getByLabelText("Vertical (%)")).toHaveValue(21.2);

    fireEvent.keyDown(nome, { key: "ArrowRight" });
    fireEvent.keyDown(nome, { key: "ArrowDown", shiftKey: true });
    fireEvent.keyDown(nome, { key: "Enter" });
    expect(screen.getByLabelText("Horizontal (%)")).toHaveValue(30.5);
    expect(screen.getByLabelText("Vertical (%)")).toHaveValue(23.2);

    fireEvent.pointerDown(nome, { clientX: 10, clientY: 10, pointerId: 2 });
    fireEvent.pointerCancel(nome, { pointerId: 2 });

    await userEvent.click(screen.getByLabelText("Mostrar no certificado"));
    expect(
      screen.queryByRole("button", { name: "Nome da pessoa: arraste para mover" }),
    ).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Voltar às posições padrão" }));
    expect(screen.getByRole("button", { name: "Nome da pessoa: arraste para mover" })).toBeInTheDocument();
    expect(screen.queryByText("Há mudanças que ainda não foram salvas.")).not.toBeInTheDocument();
  });

  it("envia e remove a arte de fundo mostrando os avisos", async () => {
    const comArte: ModeloDoCertificado = {
      ...modelo,
      temArte: true,
      nomeDaArte: "arte.png",
      atualizadoEm: "2026-11-18T10:00:00Z",
      avisos: ["A arte não tem a proporção do A4 (297 × 210 mm) e foi esticada para ocupar a página."],
    };
    const chamadas = await abrirModelo({
      "POST /admin/certificados/modelo/arte": { corpo: comArte },
      "DELETE /admin/certificados/modelo/arte": { corpo: modelo },
    });

    const arquivo = new File(["png"], "arte.png", { type: "image/png" });
    await userEvent.upload(screen.getByLabelText("Arquivo da arte"), arquivo);
    await userEvent.click(screen.getByRole("button", { name: "Enviar arte" }));

    expect(await screen.findByText("Arte em uso: arte.png")).toBeInTheDocument();
    expect(screen.getByText(/foi esticada/)).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Prévia do certificado" }).style.backgroundImage).toContain(
      enderecoDaArte("2026-11-18T10:00:00Z"),
    );
    const envio = chamadas.find((chamada) => chamada.caminho === "/admin/certificados/modelo/arte");
    expect((envio?.corpo as FormData).get("arquivo")).toBeInstanceOf(File);

    await userEvent.click(screen.getByRole("button", { name: "Remover arte" }));
    expect(await screen.findByText(/Sem arte enviada/)).toBeInTheDocument();
  });

  it("mostra quando a arte é recusada", async () => {
    await abrirModelo({
      "POST /admin/certificados/modelo/arte": {
        status: 422,
        corpo: { detail: "Envie a arte em PNG, JPG ou PDF." },
      },
    });

    await userEvent.upload(
      screen.getByLabelText("Arquivo da arte"),
      new File(["x"], "falsa.png", { type: "image/png" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Enviar arte" }));

    expect(await screen.findByText("Envie a arte em PNG, JPG ou PDF.")).toBeInTheDocument();
  });

  it("baixa o PDF de exemplo com as posições da tela", async () => {
    const criar = vi.fn(() => "blob:exemplo");
    const revogar = vi.fn();
    vi.stubGlobal("URL", Object.assign(URL, { createObjectURL: criar, revokeObjectURL: revogar }));
    const clicar = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
    const chamadas = await abrirModelo({ "POST /admin/certificados/modelo/previa": { corpo: "PDF" } });

    await userEvent.click(screen.getByRole("button", { name: "Baixar PDF de exemplo" }));

    await vi.waitFor(() => expect(clicar).toHaveBeenCalled());
    expect(criar).toHaveBeenCalled();
    expect(revogar).toHaveBeenCalledWith("blob:exemplo");
    expect(camposEnviados(chamadas.find((chamada) => chamada.metodo === "POST")?.corpo)).toHaveLength(5);
  });

  it("mostra o erro do PDF de exemplo e do salvamento", async () => {
    await abrirModelo({
      "POST /admin/certificados/modelo/previa": {
        status: 422,
        corpo: { detail: "Use uma cor no formato #RRGGBB." },
      },
      "PUT /admin/certificados/modelo": {
        status: 422,
        corpo: { detail: "Informe a posição de todos os campos." },
      },
    });

    await userEvent.click(screen.getByRole("button", { name: "Baixar PDF de exemplo" }));
    expect(await screen.findByText("Use uma cor no formato #RRGGBB.")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Tamanho da letra"), { target: { value: "20" } });
    await userEvent.click(screen.getByRole("button", { name: "Salvar posições" }));
    expect(await screen.findByText("Informe a posição de todos os campos.")).toBeInTheDocument();
  });

  it("mostra o erro ao carregar o modelo", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/certificados?pagina=0": { corpo: pagina(1) },
      "GET /admin/certificados/modelo": { status: 500, corpo: { detail: "Modelo indisponível." } },
    });
    abrir();

    await userEvent.click(await screen.findByRole("tab", { name: "Modelo" }));
    expect(await screen.findByText("Modelo indisponível.")).toBeInTheDocument();
  });
});

describe("endereços dos certificados", () => {
  it("monta o ZIP só com os filtros preenchidos e a arte com a versão", () => {
    expect(enderecoDoZip({ moduloId: 1, setorId: undefined, busca: "" })).toBe(
      "/api/admin/certificados/exportar?moduloId=1",
    );
    expect(enderecoDaArte()).toBe("/api/admin/certificados/modelo/arte");
  });
});

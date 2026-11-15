import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { enderecoDaExportacao, parametros } from "../../api/relatorios";
import type {
  Conformidade,
  ContaLogada,
  Desempenho,
  Engajamento,
  FiltrosDosRelatorios,
  PaginaDePessoas,
} from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { BarraDePercentual, BarraEmpilhada, GraficoDeColunas } from "./graficos";
import { ContextoDaConta, formatarPercentual } from "./painel";
import { TelaDeRelatorios } from "./TelaDeRelatorios";

const auditor: ContaLogada = {
  id: 2,
  nome: "Rui Auditor",
  usuario: "rui",
  perfil: "AUDITOR",
  permissoes: ["VER_RELATORIOS", "EXPORTAR_RELATORIOS", "VER_AUDITORIA"],
  precisaTrocarSenha: false,
};

const gestor: ContaLogada = {
  ...auditor,
  id: 3,
  perfil: "GESTOR",
  permissoes: ["VER_RELATORIOS", "EXPORTAR_RELATORIOS"],
};

const semExportar: ContaLogada = { ...gestor, permissoes: ["VER_RELATORIOS"] };

const filtros: FiltrosDosRelatorios = {
  setores: [
    { id: 5, nome: "TI", empresa: "Unimed Exemplo" },
    { id: 6, nome: "Faturamento", empresa: "Unimed Exemplo" },
  ],
  modulos: [
    { id: 1, titulo: "LGPD" },
    { id: 2, titulo: "Uso de IA" },
  ],
};

const conformidade: Conformidade = {
  pessoas: 3,
  totalDeLicoes: 4,
  trilha: { concluiram: 1, emAndamento: 1, naoComecaram: 1 },
  modulos: [
    { id: 1, titulo: "LGPD", licoes: 3, concluiram: 2, emAndamento: 0, naoComecaram: 1 },
    { id: 2, titulo: "Uso de IA", licoes: 1, concluiram: 1, emAndamento: 1, naoComecaram: 1 },
  ],
  setores: [
    {
      id: 5,
      setor: "TI",
      empresa: "Unimed Exemplo",
      pessoas: 2,
      concluiramATrilha: 1,
      licoesConcluidas: 62.5,
    },
  ],
};

const conformidadeDaTi: Conformidade = { ...conformidade, pessoas: 2, modulos: [] };

function pessoas(total: number, pagina = 0): PaginaDePessoas {
  return {
    pagina,
    tamanho: 50,
    total,
    itens: [
      {
        id: 10 + pagina,
        nome: pagina === 0 ? "Ana Exemplo" : "Bia Exemplo",
        setor: "TI",
        empresa: "Unimed Exemplo",
        licoesConcluidas: 4,
        totalDeLicoes: 4,
        modulosConcluidos: 2,
        totalDeModulos: 2,
        situacao: "CONCLUIU",
        ultimaAtividade: "2026-11-13T12:00:00Z",
      },
      {
        id: 20 + pagina,
        nome: "Caio Exemplo",
        setor: "Faturamento",
        empresa: "Unimed Exemplo",
        licoesConcluidas: 0,
        totalDeLicoes: 4,
        modulosConcluidos: 0,
        totalDeModulos: 2,
        situacao: "NAO_COMECOU",
      },
    ],
  };
}

const desempenho: Desempenho = {
  de: "2026-10-16",
  ate: "2026-11-15",
  licoes: [
    {
      id: 7,
      modulo: "LGPD",
      licao: "O que são dados pessoais",
      iniciadas: 4,
      concluidas: 2,
      perfeitas: 1,
      abandonadas: 1,
      primeirasRespostas: 8,
      acertoNaPrimeira: 50,
      abandono: 25,
    },
  ],
  desafios: [
    {
      id: 30,
      modulo: "LGPD",
      licao: "O que são dados pessoais",
      enunciado: "O CPF é um dado pessoal?",
      tipo: "VERDADEIRO_OU_FALSO",
      dificuldade: "FACIL",
      respostas: 12,
      acertos: 6,
      acerto: 50,
      dificuldadeObservada: "MEDIO",
      calibracao: "MAIS_DIFICIL_QUE_O_PREVISTO",
      erroMaisComum: "Falso",
      vezesDoErroMaisComum: 6,
    },
    {
      id: 31,
      enunciado: "Quem é o encarregado?",
      tipo: "MULTIPLA_ESCOLHA",
      dificuldade: "DIFICIL",
      respostas: 1,
      acertos: 1,
      acerto: 100,
      calibracao: "POUCOS_DADOS",
      erroMaisComum: "O estagiário",
      vezesDoErroMaisComum: 1,
    },
  ],
};

const engajamento: Engajamento = {
  pessoasNoEscopo: 3,
  semanas: [
    {
      inicio: "2026-11-02",
      pessoasAtivas: 1,
      licoesConcluidas: 2,
      novosParticipantes: 1,
      participacao: 33.3,
    },
    {
      inicio: "2026-11-09",
      pessoasAtivas: 2,
      licoesConcluidas: 5,
      novosParticipantes: 1,
      participacao: 66.7,
    },
  ],
};

const BASE = {
  "GET /admin/relatorios/filtros": { corpo: filtros },
  "GET /admin/relatorios/conformidade": { corpo: conformidade },
  "GET /admin/relatorios/conformidade/pessoas?pagina=0": { corpo: pessoas(2) },
};

function abrir(conta: ContaLogada = auditor) {
  renderizar(
    <ContextoDaConta value={conta}>
      <TelaDeRelatorios />
    </ContextoDaConta>,
  );
}

describe("TelaDeRelatorios", () => {
  it("mostra a conformidade com números, gráficos e tabela", async () => {
    simularApi(BASE);
    abrir();

    expect(await screen.findByRole("img", { name: /Módulo LGPD\. Concluíram: 2/ })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Conformidade" })).toHaveAttribute("aria-selected", "true");
    expect(screen.getByText("Pessoas ativas").nextSibling).toHaveTextContent("3");
    expect(screen.getByText("(3 lições)")).toBeInTheDocument();
    expect(screen.getByText("(1 lição)")).toBeInTheDocument();
    expect(screen.getByRole("meter", { name: "Lições feitas em TI" })).toHaveAttribute(
      "aria-valuetext",
      "62,5%",
    );
    expect(screen.queryByText(/Você vê apenas/)).not.toBeInTheDocument();

    expect(await screen.findByText("2 pessoas")).toBeInTheDocument();
    const linhaDoCaio = screen.getByText("Caio Exemplo").closest("tr");
    expect(linhaDoCaio).not.toBeNull();
    expect(within(linhaDoCaio as HTMLElement).getByText("Nunca")).toBeInTheDocument();
    expect(within(linhaDoCaio as HTMLElement).getByText("Não começou")).toBeInTheDocument();
    expect(screen.getByText("0 de 4")).toBeInTheDocument();
  });

  it("filtra por setor e mostra o recorte do gestor", async () => {
    simularApi({
      ...BASE,
      "GET /admin/relatorios/filtros": { corpo: { ...filtros, setores: [filtros.setores[0]] } },
      "GET /admin/relatorios/conformidade?setorId=5": { corpo: conformidadeDaTi },
      "GET /admin/relatorios/conformidade/pessoas?pagina=0&setorId=5": { corpo: pessoas(1) },
    });
    abrir(gestor);

    expect(await screen.findByText(/Você vê apenas: TI\./)).toBeInTheDocument();
    await userEvent.selectOptions(await screen.findByLabelText("Setor"), "5");

    expect(await screen.findByText("Nenhum módulo publicado ainda.")).toBeInTheDocument();
    expect(await screen.findByText("1 pessoa")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar a lista em CSV" })).toHaveAttribute(
      "href",
      "/api/admin/relatorios/conformidade-pessoas/exportar?setorId=5&formato=CSV",
    );
  });

  it("filtra e pagina a lista de pessoas", async () => {
    const chamadas = simularApi({
      ...BASE,
      "GET /admin/relatorios/conformidade/pessoas?pagina=0": { corpo: pessoas(60) },
      "GET /admin/relatorios/conformidade/pessoas?pagina=1": { corpo: pessoas(60, 1) },
      "GET /admin/relatorios/conformidade/pessoas?pagina=0&moduloId=2": { corpo: pessoas(60) },
      "GET /admin/relatorios/conformidade/pessoas?pagina=0&moduloId=2&situacao=EM_ANDAMENTO": {
        corpo: pessoas(60),
      },
      "GET /admin/relatorios/conformidade/pessoas?pagina=0&moduloId=2&situacao=EM_ANDAMENTO&busca=ana": {
        corpo: pessoas(2),
      },
    });
    abrir();

    expect(await screen.findByText("Página 1 de 2")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByText("Bia Exemplo")).toBeInTheDocument();
    expect(screen.getByText("Página 2 de 2")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Anterior" }));
    expect(await screen.findByText("Ana Exemplo")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Módulo"), "2");
    await userEvent.selectOptions(screen.getByLabelText("Situação"), "EM_ANDAMENTO");
    await userEvent.type(screen.getByLabelText("Nome"), "  ana ");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(await screen.findByText("2 pessoas")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Páginas da lista" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar a lista em Excel" })).toHaveAttribute(
      "href",
      "/api/admin/relatorios/conformidade-pessoas/exportar?moduloId=2&situacao=EM_ANDAMENTO&busca=ana&formato=XLSX",
    );
    expect(chamadas.some((chamada) => chamada.caminho.endsWith("busca=ana"))).toBe(true);
  });

  it("mostra o desempenho por lição e por desafio e aplica o período", async () => {
    simularApi({
      ...BASE,
      "GET /admin/relatorios/desempenho": { corpo: desempenho },
      "GET /admin/relatorios/desempenho?de=2026-10-01&ate=2026-10-31&moduloId=1": {
        corpo: { ...desempenho, de: "2026-10-01", ate: "2026-10-31", licoes: [], desafios: [] },
      },
    });
    abrir();

    await userEvent.click(await screen.findByRole("tab", { name: "Desempenho" }));

    expect(await screen.findByText("De 16/10 a 15/11")).toBeInTheDocument();
    const linha = screen
      .getByText("O que são dados pessoais", { selector: "span" })
      .closest("tr") as HTMLElement;
    expect(within(linha).getByText("25%")).toBeInTheDocument();
    expect(
      within(linha).getByRole("meter", { name: "Acerto na primeira resposta em O que são dados pessoais" }),
    ).toHaveAttribute("aria-valuetext", "50%");

    expect(screen.getByText("Mais difícil que o previsto")).toBeInTheDocument();
    expect(screen.getByText("Cadastrado como Fácil; na prática está Médio.")).toBeInTheDocument();
    expect(screen.getByText("Falso")).toBeInTheDocument();
    expect(screen.getByText("12 primeiras respostas", { exact: false })).toBeInTheDocument();
    expect(screen.getByText("Poucos dados")).toBeInTheDocument();
    expect(screen.getByText("Cadastrado como Difícil.")).toBeInTheDocument();
    expect(screen.getByText("1 primeira resposta", { exact: false })).toBeInTheDocument();
    expect(screen.getByText(/\(1 vez\)/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar os desafios em CSV" })).toHaveAttribute(
      "href",
      "/api/admin/relatorios/desempenho-desafios/exportar?formato=CSV",
    );

    await userEvent.type(screen.getByLabelText("De"), "2026-10-01");
    await userEvent.type(screen.getByLabelText("Até"), "2026-10-31");
    await userEvent.selectOptions(screen.getByLabelText("Módulo do desempenho"), "1");
    await userEvent.click(screen.getByRole("button", { name: "Aplicar" }));

    expect(await screen.findByText("Ninguém fez lições neste período.")).toBeInTheDocument();
    expect(screen.getByText("Nenhuma resposta neste período.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar as lições em Excel" })).toHaveAttribute(
      "href",
      "/api/admin/relatorios/desempenho-licoes/exportar?de=2026-10-01&ate=2026-10-31&moduloId=1&formato=XLSX",
    );
  });

  it("mostra o engajamento por semana e troca o período", async () => {
    simularApi({
      ...BASE,
      "GET /admin/relatorios/engajamento?semanas=12": { corpo: engajamento },
      "GET /admin/relatorios/engajamento?semanas=4": { corpo: { ...engajamento, semanas: [] } },
    });
    abrir();

    await userEvent.click(await screen.findByRole("tab", { name: "Engajamento" }));

    expect(
      await screen.findByText("Pessoas que concluíram ao menos uma lição, de 3 ativas"),
    ).toBeInTheDocument();
    const tabela = screen.getByRole("table", { name: "Engajamento por semana" });
    expect(within(tabela).getByText("09/11")).toBeInTheDocument();
    expect(within(tabela).getByText("66,7%")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Baixar o engajamento em CSV" })).toHaveAttribute(
      "href",
      "/api/admin/relatorios/engajamento/exportar?semanas=12&formato=CSV",
    );

    await userEvent.selectOptions(screen.getByLabelText("Período"), "4");
    expect(await screen.findByRole("link", { name: "Baixar o engajamento em Excel" })).toHaveAttribute(
      "href",
      "/api/admin/relatorios/engajamento/exportar?semanas=4&formato=XLSX",
    );
    expect(
      within(screen.getByRole("table", { name: "Engajamento por semana" })).queryByText("09/11"),
    ).toBeNull();
  });

  it("esconde a exportação de quem só pode ver", async () => {
    simularApi(BASE);
    abrir(semExportar);

    expect(await screen.findByText("2 pessoas")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /Baixar/ })).not.toBeInTheDocument();
  });

  it("avisa quando os relatórios não carregam", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { status: 403, corpo: { detail: "Sem permissão." } },
    });
    abrir();

    expect(await screen.findByRole("alert")).toHaveTextContent("Sem permissão.");
  });

  it("avisa quando uma aba falha", async () => {
    simularApi({
      "GET /admin/relatorios/filtros": { corpo: filtros },
      "GET /admin/relatorios/conformidade": { status: 500, corpo: { detail: "Falhou a conformidade." } },
      "GET /admin/relatorios/conformidade/pessoas?pagina=0": {
        status: 500,
        corpo: { detail: "Falhou a lista." },
      },
      "GET /admin/relatorios/desempenho": { status: 422, corpo: { detail: "Período inválido." } },
      "GET /admin/relatorios/engajamento?semanas=12": {
        status: 500,
        corpo: { detail: "Falhou o engajamento." },
      },
    });
    abrir();

    expect(await screen.findByText("Falhou a conformidade.")).toBeInTheDocument();
    expect(await screen.findByText("Falhou a lista.")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("tab", { name: "Desempenho" }));
    expect(await screen.findByText("Período inválido.")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("tab", { name: "Engajamento" }));
    expect(await screen.findByText("Falhou o engajamento.")).toBeInTheDocument();
  });
});

describe("gráficos dos relatórios", () => {
  it("descrevem os números em texto", () => {
    renderizar(
      <div>
        <BarraEmpilhada
          rotulo="Trilha"
          fatias={[
            { rotulo: "Sim", valor: 3, cor: "bg-primaria" },
            { rotulo: "Não", valor: 0, cor: "bg-neutro" },
          ]}
        />
        <BarraDePercentual rotulo="Sem dados" valor={undefined} />
        <BarraDePercentual rotulo="Acima" valor={120} />
        <GraficoDeColunas rotulo="Vazio" colunas={[{ rotulo: "a", valor: 0, detalhe: "zero" }]} />
      </div>,
    );

    expect(screen.getByRole("img", { name: "Trilha. Sim: 3, Não: 0" }).children).toHaveLength(1);
    expect(screen.getByRole("meter", { name: "Sem dados" })).toHaveAttribute("aria-valuetext", "—");
    expect(screen.getByRole("meter", { name: "Acima" }).firstElementChild).toHaveStyle({ width: "100%" });
    expect(screen.getByTitle("zero")).toHaveStyle({ height: "0%" });
  });

  it("formatam percentuais e montam os endereços", () => {
    expect(formatarPercentual(null)).toBe("—");
    expect(formatarPercentual(33.333)).toBe("33,3%");
    expect(parametros({ a: "", b: null, c: undefined, d: 0, e: "x" })).toBe("d=0&e=x");
    expect(enderecoDaExportacao("engajamento", "CSV")).toBe(
      "/api/admin/relatorios/engajamento/exportar?formato=CSV",
    );
  });
});

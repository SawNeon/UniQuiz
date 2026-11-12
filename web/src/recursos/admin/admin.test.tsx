import { fireEvent, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { DesafioParaAdmin, ModuloParaAdmin } from "../../api/tipos";
import { renderizar, simularApi } from "../../testes/utilidades";
import { EditorDeDesafio } from "./EditorDeDesafio";
import { LayoutAdmin } from "./LayoutAdmin";
import { LoginAdmin } from "./LoginAdmin";
import { TelaDaLicaoAdmin } from "./TelaDaLicaoAdmin";
import { TelaDeColaboradores } from "./TelaDeColaboradores";
import { TelaDeConteudo } from "./TelaDeConteudo";
import { TelaDeTemporadas } from "./TelaDeTemporadas";

const modulos: ModuloParaAdmin[] = [
  {
    id: 1,
    titulo: "LGPD na prática",
    descricao: "",
    materialEstudo: "# LGPD",
    ordem: 1,
    status: "PUBLICADO",
    licoes: [
      {
        id: 5,
        moduloId: 1,
        titulo: "O básico da LGPD",
        ordem: 1,
        status: "PUBLICADO",
        desafiosPublicados: 5,
        quantidades: { facil: 2, medio: 2, dificil: 1 },
        publicadosPorNivel: { facil: 3, medio: 2, dificil: 0 },
      },
    ],
  },
  { id: 2, titulo: "IA com responsabilidade", descricao: "", ordem: 2, status: "RASCUNHO", licoes: [] },
];

describe("LoginAdmin", () => {
  it("valida os campos antes de enviar", async () => {
    const chamadas = simularApi({});
    renderizar(<LoginAdmin />, { rota: "/admin/entrar", caminho: "/admin/entrar" });

    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Informe o usuário.")).toBeInTheDocument();
    expect(screen.getByText("Informe a senha.")).toBeInTheDocument();
    expect(chamadas).toHaveLength(0);
  });

  it("entra e vai para o painel", async () => {
    const chamadas = simularApi({ "POST /admin/login": {} });
    renderizar(<LoginAdmin />, { rota: "/admin/entrar", caminho: "/admin/entrar" });

    await userEvent.type(screen.getByLabelText("Usuário"), "admin");
    await userEvent.type(screen.getByLabelText("Senha"), "senha-secreta");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Página: /admin")).toBeInTheDocument();
    expect(chamadas[0]?.corpo).toEqual({ usuario: "admin", senha: "senha-secreta" });
  });

  it("mostra a recusa do servidor", async () => {
    simularApi({ "POST /admin/login": { status: 422, corpo: { detail: "Usuário ou senha inválidos." } } });
    renderizar(<LoginAdmin />, { rota: "/admin/entrar", caminho: "/admin/entrar" });

    await userEvent.type(screen.getByLabelText("Usuário"), "admin");
    await userEvent.type(screen.getByLabelText("Senha"), "errada");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Usuário ou senha inválidos.");
  });
});

describe("LayoutAdmin", () => {
  it("manda para o login quem não é admin", async () => {
    simularApi({ "GET /admin/eu": { status: 401 } });
    renderizar(<LayoutAdmin />, { rota: "/admin", caminho: "/admin" });

    expect(await screen.findByText("Página: /admin/entrar")).toBeInTheDocument();
  });

  it("mostra o menu e sai", async () => {
    simularApi({ "GET /admin/eu": { corpo: { id: 1 } }, "DELETE /admin/sessao": {} });
    renderizar(<LayoutAdmin />, { rota: "/admin", caminho: "/admin" });

    expect(await screen.findByRole("link", { name: "Colaboradores" })).toHaveAttribute(
      "href",
      "/admin/colaboradores",
    );
    await userEvent.click(screen.getByRole("button", { name: "Sair" }));
    expect(await screen.findByText("Página: /admin/entrar")).toBeInTheDocument();
  });
});

describe("TelaDeConteudo", () => {
  it("lista o caminho, muda a situação, reordena e cria lição", async () => {
    const chamadas = simularApi({
      "GET /admin/modulos": { corpo: modulos },
      "POST /admin/modulos/2/status": {},
      "PATCH /admin/modulos/ordem": {},
      "POST /admin/modulos/2/licoes": { status: 201, corpo: {} },
    });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });

    expect(await screen.findByRole("heading", { name: "LGPD na prática" })).toBeInTheDocument();
    expect(screen.getByText("5 desafios publicados")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Desafios" })).toHaveAttribute("href", "/admin/licoes/5");

    await userEvent.selectOptions(
      screen.getByLabelText("Situação do módulo IA com responsabilidade"),
      "PUBLICADO",
    );
    await userEvent.click(screen.getByRole("button", { name: "Subir o módulo IA com responsabilidade" }));
    await userEvent.type(
      screen.getByLabelText("Título da nova lição em IA com responsabilidade"),
      "IA no trabalho",
    );
    await userEvent.click(
      within(screen.getByRole("region", { name: "IA com responsabilidade" })).getByRole("button", {
        name: "Adicionar lição",
      }),
    );

    await screen.findByRole("heading", { name: "LGPD na prática" });
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/admin/modulos/2/status",
      corpo: { status: "PUBLICADO" },
    });
    expect(chamadas).toContainEqual({
      metodo: "PATCH",
      caminho: "/admin/modulos/ordem",
      corpo: { ids: [2, 1] },
    });
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/admin/modulos/2/licoes",
      corpo: { titulo: "IA no trabalho", quantidades: { facil: 2, medio: 2, dificil: 1 } },
    });
  });

  it("cria a lição com as quantidades por nível que o admin escolher", async () => {
    const chamadas = simularApi({
      "GET /admin/modulos": { corpo: modulos },
      "POST /admin/modulos/2/licoes": { status: 201, corpo: {} },
    });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });
    const modulo = within(await screen.findByRole("region", { name: "IA com responsabilidade" }));

    await userEvent.type(
      modulo.getByLabelText("Título da nova lição em IA com responsabilidade"),
      "Phishing",
    );
    fireEvent.change(modulo.getByLabelText("Fáceis por pessoa na nova lição"), { target: { value: "5" } });
    fireEvent.change(modulo.getByLabelText("Médios por pessoa na nova lição"), { target: { value: "5" } });
    fireEvent.change(modulo.getByLabelText("Difíceis por pessoa na nova lição"), { target: { value: "5" } });
    expect(modulo.getByText("15")).toBeInTheDocument();
    await userEvent.click(modulo.getByRole("button", { name: "Adicionar lição" }));

    await vi.waitFor(() =>
      expect(chamadas).toContainEqual({
        metodo: "POST",
        caminho: "/admin/modulos/2/licoes",
        corpo: { titulo: "Phishing", quantidades: { facil: 5, medio: 5, dificil: 5 } },
      }),
    );
  });

  it("limita a quantidade de 0 ao máximo por nível", async () => {
    simularApi({ "GET /admin/modulos": { corpo: modulos } });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });
    const modulo = within(await screen.findByRole("region", { name: "IA com responsabilidade" }));

    fireEvent.change(modulo.getByLabelText("Fáceis por pessoa na nova lição"), { target: { value: "99" } });
    fireEvent.change(modulo.getByLabelText("Médios por pessoa na nova lição"), { target: { value: "-4" } });
    fireEvent.change(modulo.getByLabelText("Difíceis por pessoa na nova lição"), { target: { value: "" } });

    expect(modulo.getByLabelText("Fáceis por pessoa na nova lição")).toHaveValue(30);
    expect(modulo.getByLabelText("Médios por pessoa na nova lição")).toHaveValue(0);
    expect(modulo.getByLabelText("Difíceis por pessoa na nova lição")).toHaveValue(0);
  });

  it("mostra o banco de cada nível e avisa quando falta desafio para o sorteio", async () => {
    simularApi({ "GET /admin/modulos": { corpo: modulos } });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });
    const modulo = within(await screen.findByRole("region", { name: "LGPD na prática" }));

    expect(modulo.getByText("Banco: 3 publicados")).toBeInTheDocument();
    expect(modulo.getByText("Banco: 2 publicados")).toBeInTheDocument();
    expect(modulo.getByText("Banco: 0 publicados (faltam desafios)")).toBeInTheDocument();
  });

  it("ajusta o sorteio de uma lição que já existe ao sair do campo", async () => {
    const chamadas = simularApi({
      "GET /admin/modulos": { corpo: modulos },
      "PUT /admin/licoes/5": { corpo: {} },
    });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });
    const modulo = within(await screen.findByRole("region", { name: "LGPD na prática" }));

    const faceis = modulo.getByLabelText("Fáceis por pessoa em O básico da LGPD");
    fireEvent.change(faceis, { target: { value: "3" } });
    fireEvent.blur(faceis);

    await vi.waitFor(() =>
      expect(chamadas).toContainEqual({
        metodo: "PUT",
        caminho: "/admin/licoes/5",
        corpo: { titulo: "O básico da LGPD", quantidades: { facil: 3, medio: 2, dificil: 1 } },
      }),
    );
  });

  it("não salva quando o campo não mudou", async () => {
    const chamadas = simularApi({ "GET /admin/modulos": { corpo: modulos } });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });
    const modulo = within(await screen.findByRole("region", { name: "LGPD na prática" }));

    fireEvent.blur(modulo.getByLabelText("Fáceis por pessoa em O básico da LGPD"));

    expect(chamadas.filter((chamada) => chamada.metodo === "PUT")).toHaveLength(0);
  });

  it("mostra a recusa do servidor e volta ao valor anterior", async () => {
    simularApi({
      "GET /admin/modulos": { corpo: modulos },
      "PUT /admin/licoes/5": {
        status: 422,
        corpo: { detail: "Faltam desafios no nível difícil." },
      },
    });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });
    const modulo = within(await screen.findByRole("region", { name: "LGPD na prática" }));

    const dificeis = modulo.getByLabelText("Difíceis por pessoa em O básico da LGPD");
    fireEvent.change(dificeis, { target: { value: "4" } });
    fireEvent.blur(dificeis);

    expect(await screen.findByRole("alert")).toHaveTextContent("Faltam desafios no nível difícil.");
    expect(modulo.getByLabelText("Difíceis por pessoa em O básico da LGPD")).toHaveValue(1);
  });

  it("cria módulo com material de estudo e mostra a pré-visualização", async () => {
    const chamadas = simularApi({
      "GET /admin/modulos": { corpo: [] },
      "POST /admin/modulos": { status: 201, corpo: {} },
    });
    renderizar(<TelaDeConteudo />, { rota: "/admin", caminho: "/admin" });

    await userEvent.click(await screen.findByRole("button", { name: "Novo módulo" }));
    await userEvent.type(screen.getByLabelText("Título"), "Golpes digitais");
    fireEvent.change(screen.getByLabelText(/Conteúdo para estudar/), {
      target: { value: "# Golpes\n\nDesconfie de **urgência**." },
    });

    expect(
      within(screen.getByRole("region", { name: "Pré-visualização do conteúdo" })).getByText("urgência")
        .tagName,
    ).toBe("STRONG");
    await userEvent.click(screen.getByRole("button", { name: "Salvar módulo" }));

    await screen.findByRole("button", { name: "Novo módulo" });
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/admin/modulos",
      corpo: {
        titulo: "Golpes digitais",
        descricao: "",
        materialEstudo: "# Golpes\n\nDesconfie de **urgência**.",
      },
    });
  });
});

describe("EditorDeDesafio", () => {
  it("transforma a palavra selecionada em lacuna, monta o banco e salva", async () => {
    const chamadas = simularApi({ "POST /admin/licoes/5/desafios": { status: 201, corpo: {} } });
    const aoTerminar = vi.fn();
    renderizar(<EditorDeDesafio licaoId={5} aoTerminar={aoTerminar} />);

    await userEvent.selectOptions(screen.getByLabelText("Tipo"), "COMPLETAR_CLICANDO");
    await userEvent.type(screen.getByLabelText("Enunciado"), "Complete a frase.");
    const frase = screen.getByLabelText("Frase completa") as HTMLTextAreaElement;
    fireEvent.change(frase, { target: { value: "Use apenas ferramentas aprovadas pela TI." } });

    await userEvent.click(screen.getByRole("button", { name: "Transformar em lacuna" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Selecione com o mouse");

    const inicio = frase.value.indexOf("aprovadas");
    frase.setSelectionRange(inicio, inicio + "aprovadas".length);
    await userEvent.click(screen.getByRole("button", { name: "Transformar em lacuna" }));

    expect(frase.value).toBe("Use apenas ferramentas {{1}} pela TI.");
    expect(screen.getByText("Lacuna 1: “aprovadas”")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Adicionar palavra errada" }));
    await userEvent.type(screen.getByLabelText("Palavra errada 1 da lacuna 1"), "gratuitas");
    await userEvent.type(
      screen.getByLabelText("Por que a palavra 1 da lacuna 1 está errada"),
      "Gratuita não é segura.",
    );
    await userEvent.type(screen.getByLabelText(/Explicação geral/), "A TI avalia cada ferramenta.");

    const previa = screen.getByRole("region", { name: "Pré-visualização do desafio" });
    await userEvent.click(within(previa).getByRole("button", { name: "aprovadas" }));
    await userEvent.click(within(previa).getByRole("button", { name: "Verificar" }));
    expect(within(previa).getByRole("status")).toHaveTextContent("Muito bem!");

    await userEvent.click(screen.getByRole("button", { name: "Salvar desafio" }));
    await vi.waitFor(() => expect(aoTerminar).toHaveBeenCalled());
    expect(chamadas[0]?.corpo).toMatchObject({
      tipo: "COMPLETAR_CLICANDO",
      enunciado: "Complete a frase.",
      conteudo: {
        texto: "Use apenas ferramentas {{1}} pela TI.",
        lacunas: [
          {
            id: 1,
            banco: [
              { palavra: "aprovadas", correta: true, motivo: "" },
              { palavra: "gratuitas", correta: false, motivo: "Gratuita não é segura." },
            ],
          },
        ],
      },
    });
  });

  it("desfaz a lacuna devolvendo a palavra para a frase", async () => {
    simularApi({});
    renderizar(<EditorDeDesafio licaoId={5} aoTerminar={vi.fn()} />);
    await userEvent.selectOptions(screen.getByLabelText("Tipo"), "COMPLETAR_CLICANDO");
    const frase = screen.getByLabelText("Frase completa") as HTMLTextAreaElement;
    fireEvent.change(frase, { target: { value: "Bloqueie a tela." } });
    frase.setSelectionRange(10, 14);
    await userEvent.click(screen.getByRole("button", { name: "Transformar em lacuna" }));

    await userEvent.click(screen.getByRole("button", { name: "Desfazer lacuna" }));

    expect(frase.value).toBe("Bloqueie a tela.");
  });

  it("edita múltipla escolha marcando uma só certa e avisa sobre nova versão", async () => {
    const desafio: DesafioParaAdmin = {
      id: 8,
      licaoId: 5,
      tipo: "MULTIPLA_ESCOLHA",
      enunciado: "Qual a certa?",
      dificuldade: "FACIL",
      pontos: 10,
      explicacao: "Porque sim.",
      versao: 2,
      ordem: 1,
      status: "PUBLICADO",
      conteudo: {
        alternativas: [
          { id: "a", texto: "Primeira", correta: true, motivo: "" },
          { id: "b", texto: "Segunda", correta: false, motivo: "Não é." },
        ],
      },
    };
    const chamadas = simularApi({ "PUT /admin/desafios/8": { corpo: {} } });
    renderizar(<EditorDeDesafio licaoId={5} desafio={desafio} aoTerminar={vi.fn()} />);

    expect(screen.getByText(/Salvar cria a versão 3/)).toBeInTheDocument();
    expect(screen.getByLabelText("Tipo")).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Adicionar alternativa" }));
    await userEvent.type(screen.getByLabelText("Texto da alternativa C"), "Terceira");
    await userEvent.click(screen.getByLabelText("Alternativa C é a certa"));
    await userEvent.type(screen.getByLabelText("Por que a alternativa A está errada"), "Era antes.");
    await userEvent.click(screen.getByRole("button", { name: "Remover a alternativa B" }));
    await userEvent.click(screen.getByRole("button", { name: "Salvar desafio" }));

    await vi.waitFor(() => expect(chamadas).toHaveLength(1));
    expect(chamadas[0]?.corpo).toMatchObject({
      conteudo: {
        alternativas: [
          { id: "a", texto: "Primeira", correta: false, motivo: "Era antes." },
          { id: "c", texto: "Terceira", correta: true, motivo: "" },
        ],
      },
    });
  });

  it("mostra a recusa do servidor ao salvar", async () => {
    simularApi({
      "POST /admin/licoes/5/desafios": {
        status: 422,
        corpo: { detail: "Marque exatamente uma alternativa correta." },
      },
    });
    renderizar(<EditorDeDesafio licaoId={5} aoTerminar={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "Salvar desafio" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Marque exatamente uma alternativa correta.");
  });

  it("verdadeiro ou falso guarda se a afirmação é falsa", async () => {
    const chamadas = simularApi({ "POST /admin/licoes/5/desafios": { status: 201, corpo: {} } });
    renderizar(<EditorDeDesafio licaoId={5} aoTerminar={vi.fn()} />);

    await userEvent.selectOptions(screen.getByLabelText("Tipo"), "VERDADEIRO_OU_FALSO");
    await userEvent.type(screen.getByLabelText("Afirmação"), "Senha curta é segura.");
    await userEvent.selectOptions(screen.getByLabelText("A afirmação é"), "false");
    await userEvent.click(screen.getByRole("button", { name: "Salvar desafio" }));

    await vi.waitFor(() => expect(chamadas).toHaveLength(1));
    expect(chamadas[0]?.corpo).toMatchObject({
      tipo: "VERDADEIRO_OU_FALSO",
      conteudo: { afirmacao: "Senha curta é segura.", verdadeira: false },
    });
  });
});

describe("TelaDaLicaoAdmin", () => {
  it("lista os desafios e duplica", async () => {
    const chamadas = simularApi({
      "GET /admin/licoes/5/desafios": {
        corpo: [
          {
            id: 8,
            licaoId: 5,
            tipo: "VERDADEIRO_OU_FALSO",
            enunciado: "Dados de saúde são sensíveis?",
            dificuldade: "FACIL",
            pontos: 10,
            explicacao: "",
            versao: 1,
            ordem: 1,
            status: "PUBLICADO",
            conteudo: { afirmacao: "x", verdadeira: true, motivoDoErro: "y" },
          },
        ],
      },
      "GET /admin/modulos": { corpo: modulos },
      "POST /admin/desafios/8/duplicar": { status: 201, corpo: {} },
    });
    renderizar(<TelaDaLicaoAdmin />, { rota: "/admin/licoes/5", caminho: "/admin/licoes/:licaoId" });

    expect(await screen.findByRole("heading", { name: "O básico da LGPD" })).toBeInTheDocument();
    expect(screen.getByText("Verdadeiro ou falso · Fácil · 10 pontos · versão 1")).toBeInTheDocument();
    const sorteio = within(await screen.findByRole("list", { name: "Sorteio da lição" }));
    expect(sorteio.getByText("Fáceis: sorteia 2 de 3 publicados")).toBeInTheDocument();
    expect(sorteio.getByText("Médios: sorteia 2 de 2 publicados")).toBeInTheDocument();
    expect(sorteio.getByText("Difíceis: sorteia 1 de 0 publicados (faltam desafios)")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Duplicar" }));

    await vi.waitFor(() =>
      expect(chamadas.map((chamada) => `${chamada.metodo} ${chamada.caminho}`)).toContain(
        "POST /admin/desafios/8/duplicar",
      ),
    );
    await userEvent.click(screen.getByRole("button", { name: "Novo desafio" }));
    expect(screen.getByRole("button", { name: "Salvar desafio" })).toBeInTheDocument();
  });
});

describe("TelaDeColaboradores", () => {
  it("faz a prévia da planilha e só confirma depois", async () => {
    const resumo = {
      novos: 1,
      mantidos: 2,
      reativados: 0,
      inativados: 1,
      problemas: [],
      homonimos: [],
      podeConfirmar: true,
    };
    const chamadas = simularApi({
      "GET /admin/colaboradores?busca=": { corpo: [] },
      "POST /admin/colaboradores/importacao/previa": { corpo: resumo },
      "POST /admin/colaboradores/importacao/confirmar": { corpo: resumo },
    });
    renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

    expect(screen.getByRole("button", { name: "Ver prévia" })).toBeDisabled();
    await userEvent.upload(
      screen.getByLabelText("Planilha"),
      new File(["nome,setor,empresa"], "rh.csv", { type: "text/csv" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Ver prévia" }));

    expect(await screen.findByText("Prévia da importação")).toBeInTheDocument();
    expect(screen.getByText("Novos: 1")).toBeInTheDocument();
    expect(screen.getByText("Inativados: 1")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Confirmar importação" }));
    expect(await screen.findByText("Importação concluída.")).toBeInTheDocument();
    expect(chamadas.filter((chamada) => chamada.metodo === "POST")).toHaveLength(2);
  });

  it("não deixa confirmar quando há homônimos e mostra o aviso", async () => {
    simularApi({
      "GET /admin/colaboradores?busca=": { corpo: [] },
      "POST /admin/colaboradores/importacao/previa": {
        corpo: {
          novos: 2,
          mantidos: 0,
          reativados: 0,
          inativados: 0,
          problemas: ["Linha 4: preencha nome, setor e empresa."],
          homonimos: ['Linhas 2 e 3: "João" aparece duas vezes em TI.'],
          podeConfirmar: false,
        },
      },
    });
    renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

    await userEvent.upload(screen.getByLabelText("Planilha"), new File(["x"], "rh.csv"));
    await userEvent.click(screen.getByRole("button", { name: "Ver prévia" }));

    expect(await screen.findByText(/aparece duas vezes/)).toBeInTheDocument();
    expect(screen.getByText(/preencha nome, setor e empresa/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Confirmar importação" })).not.toBeInTheDocument();
  });

  it("renomeia, inativa e zera os pontos de um colaborador", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const chamadas = simularApi({
      "GET /admin/colaboradores?busca=": {
        corpo: [
          { id: 4, nome: "Bruno Teste", setor: "TI", empresa: "Unimed Exemplo", ativo: true, temPin: true },
        ],
      },
      "PATCH /admin/colaboradores/4": {},
      "POST /admin/colaboradores/4/redefinir-pin": {},
      "POST /admin/colaboradores/4/zerar-pontos": {},
    });
    renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

    const nome = await screen.findByLabelText("Nome de Bruno Teste");
    await userEvent.clear(nome);
    await userEvent.type(nome, "Bruno Teste (TI)");
    await userEvent.click(screen.getByRole("button", { name: "Salvar nome" }));
    await userEvent.click(screen.getByRole("checkbox", { name: "Ativo" }));
    await userEvent.click(screen.getByRole("button", { name: "Zerar pontos" }));

    expect(await screen.findByText("Pontos zerados.")).toBeInTheDocument();
    expect(screen.getByText("PIN criado")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Redefinir PIN" }));
    expect(await screen.findByText("PIN apagado.")).toBeInTheDocument();
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/admin/colaboradores/4/redefinir-pin",
      corpo: undefined,
    });
    expect(chamadas).toContainEqual({
      metodo: "PATCH",
      caminho: "/admin/colaboradores/4",
      corpo: { nome: "Bruno Teste (TI)", ativo: true },
    });
    expect(chamadas).toContainEqual({
      metodo: "PATCH",
      caminho: "/admin/colaboradores/4",
      corpo: { nome: "Bruno Teste (TI)", ativo: false },
    });
  });
});

describe("TelaDeTemporadas", () => {
  it("valida as datas e cria a temporada", async () => {
    const chamadas = simularApi({
      "GET /admin/temporadas": {
        corpo: [{ id: 1, nome: "Novembro", inicio: "2026-11-01", fim: "2026-11-30", premioTexto: "Brinde" }],
      },
      "POST /admin/temporadas": { status: 201, corpo: {} },
    });
    renderizar(<TelaDeTemporadas />, { rota: "/admin/temporadas", caminho: "/admin/temporadas" });

    expect(await screen.findByText("2026-11-01 a 2026-11-30 · Prêmio: Brinde")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Nova temporada" }));
    await userEvent.type(screen.getByLabelText("Nome"), "Dezembro de 2026");
    fireEvent.change(screen.getByLabelText("Início"), { target: { value: "2026-12-31" } });
    fireEvent.change(screen.getByLabelText("Fim"), { target: { value: "2026-12-01" } });
    await userEvent.click(screen.getByRole("button", { name: "Salvar temporada" }));
    expect(await screen.findByText("O fim precisa ser depois do início.")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Início"), { target: { value: "2026-12-01" } });
    fireEvent.change(screen.getByLabelText("Fim"), { target: { value: "2026-12-31" } });
    await userEvent.click(screen.getByRole("button", { name: "Salvar temporada" }));

    await vi.waitFor(() =>
      expect(chamadas).toContainEqual({
        metodo: "POST",
        caminho: "/admin/temporadas",
        corpo: { nome: "Dezembro de 2026", inicio: "2026-12-01", fim: "2026-12-31", premioTexto: "" },
      }),
    );
  });

  it("cadastra um colaborador avulso e mostra o erro de nome repetido", async () => {
    let tentativa = 0;
    const chamadas = simularApi({
      "GET /admin/colaboradores?busca=": { corpo: [] },
      "GET /empresas": { corpo: [{ id: 1, nome: "Unimed Exemplo" }] },
      "GET /empresas/1/setores": { corpo: [{ id: 2, nome: "TI" }] },
      "POST /admin/colaboradores": () => {
        tentativa += 1;
        return tentativa === 1
          ? {
              status: 201,
              corpo: {
                id: 9,
                nome: "Diego Novo",
                setor: "Financeiro",
                empresa: "Unimed Exemplo",
                ativo: true,
                temPin: false,
              },
            }
          : { status: 422, corpo: { detail: "Já existe uma pessoa com esse nome nesse setor." } };
      },
    });
    renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

    const cadastrar = screen.getByRole("button", { name: "Cadastrar" });
    expect(cadastrar).toBeDisabled();
    await userEvent.type(screen.getByLabelText("Nome do colaborador"), "Diego Novo");
    await userEvent.type(screen.getByLabelText("Empresa do colaborador"), "unimed exemplo");
    await userEvent.type(screen.getByLabelText("Setor do colaborador"), "Financeiro");
    await userEvent.click(cadastrar);

    expect(await screen.findByText("Diego Novo cadastrado em Financeiro.")).toBeInTheDocument();
    expect(chamadas).toContainEqual({
      metodo: "POST",
      caminho: "/admin/colaboradores",
      corpo: { nome: "Diego Novo", empresa: "unimed exemplo", setor: "Financeiro" },
    });

    await userEvent.type(screen.getByLabelText("Nome do colaborador"), "Diego Novo");
    await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Já existe uma pessoa com esse nome");
  });

  describe("PIN no cadastro", () => {
    const respostaDoCadastro = {
      status: 201,
      corpo: { id: 9, nome: "Diego Novo", setor: "TI", empresa: "Unimed Exemplo", ativo: true, temPin: true },
    };

    async function preencherDadosDaPessoa() {
      await userEvent.type(screen.getByLabelText("Nome do colaborador"), "Diego Novo");
      await userEvent.type(screen.getByLabelText("Empresa do colaborador"), "Unimed Exemplo");
      await userEvent.type(screen.getByLabelText("Setor do colaborador"), "TI");
    }

    function simularCadastro() {
      return simularApi({
        "GET /admin/colaboradores?busca=": { corpo: [] },
        "GET /empresas": { corpo: [{ id: 1, nome: "Unimed Exemplo" }] },
        "GET /empresas/1/setores": { corpo: [{ id: 2, nome: "TI" }] },
        "POST /admin/colaboradores": respostaDoCadastro,
      });
    }

    it("envia o PIN junto e limpa o campo depois de cadastrar", async () => {
      const chamadas = simularCadastro();
      renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

      await preencherDadosDaPessoa();
      await userEvent.type(screen.getByLabelText("PIN do colaborador (opcional)"), "482");
      await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));

      expect(await screen.findByText("Diego Novo cadastrado em TI.")).toBeInTheDocument();
      expect(chamadas).toContainEqual({
        metodo: "POST",
        caminho: "/admin/colaboradores",
        corpo: { nome: "Diego Novo", empresa: "Unimed Exemplo", setor: "TI", pin: "482" },
      });
      expect(screen.getByLabelText("PIN do colaborador (opcional)")).toHaveValue("");
    });

    it("aceita só números e no máximo 3", async () => {
      simularCadastro();
      renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

      await userEvent.type(screen.getByLabelText("PIN do colaborador (opcional)"), "a1b2c3d4");

      expect(screen.getByLabelText("PIN do colaborador (opcional)")).toHaveValue("123");
    });

    it("não deixa cadastrar com o PIN incompleto e explica o motivo", async () => {
      const chamadas = simularCadastro();
      renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

      await preencherDadosDaPessoa();
      await userEvent.type(screen.getByLabelText("PIN do colaborador (opcional)"), "48");

      expect(screen.getByRole("button", { name: "Cadastrar" })).toBeDisabled();
      expect(screen.getByRole("alert")).toHaveTextContent("O PIN tem 3 números.");
      expect(chamadas.filter((chamada) => chamada.metodo === "POST")).toHaveLength(0);
    });

    it("sem PIN o cadastro segue como antes e não manda o campo", async () => {
      const chamadas = simularCadastro();
      renderizar(<TelaDeColaboradores />, { rota: "/admin/colaboradores", caminho: "/admin/colaboradores" });

      await preencherDadosDaPessoa();
      await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));

      await screen.findByText("Diego Novo cadastrado em TI.");
      const envio = chamadas.find((chamada) => chamada.metodo === "POST");
      expect(envio?.corpo).toEqual({ nome: "Diego Novo", empresa: "Unimed Exemplo", setor: "TI" });
    });
  });
});

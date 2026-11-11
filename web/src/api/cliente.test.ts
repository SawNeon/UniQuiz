import { simularApi } from "../testes/utilidades";
import { ErroDaApi, api, mensagemDe, naoAutenticado } from "./cliente";

describe("cliente da API", () => {
  it("envia JSON com o prefixo /api e devolve o corpo", async () => {
    const chamadas = simularApi({ "POST /sessao": { corpo: { ok: true } } });

    await expect(api.post("/sessao", { colaboradorId: 3 })).resolves.toEqual({ ok: true });

    expect(chamadas).toEqual([{ metodo: "POST", caminho: "/sessao", corpo: { colaboradorId: 3 } }]);
    const [, opcoes] = vi.mocked(fetch).mock.calls[0] ?? [];
    expect(opcoes?.headers).toMatchObject({ "Content-Type": "application/json" });
    expect(opcoes?.credentials).toBe("same-origin");
  });

  it("devolve undefined quando o servidor responde 204", async () => {
    simularApi({ "DELETE /sessao": {} });

    await expect(api.delete("/sessao")).resolves.toBeUndefined();
  });

  it("não define Content-Type ao enviar arquivo", async () => {
    simularApi({ "POST /admin/colaboradores/importacao/previa": { corpo: {} } });
    const formulario = new FormData();
    formulario.append("arquivo", new Blob(["nome,setor,empresa"]), "rh.csv");

    await api.post("/admin/colaboradores/importacao/previa", formulario);

    const [, opcoes] = vi.mocked(fetch).mock.calls[0] ?? [];
    expect(opcoes?.headers).not.toHaveProperty("Content-Type");
    expect(opcoes?.body).toBe(formulario);
  });

  it("transforma o ProblemDetail em ErroDaApi com a mensagem e os campos", async () => {
    simularApi({
      "POST /sessao": {
        status: 400,
        corpo: { detail: "Confira os campos.", campos: { colaboradorId: "obrigatório" } },
      },
    });

    const erro = await api.post("/sessao", {}).catch((falha: unknown) => falha);

    expect(erro).toBeInstanceOf(ErroDaApi);
    expect(erro).toMatchObject({
      status: 400,
      message: "Confira os campos.",
      campos: { colaboradorId: "obrigatório" },
    });
  });

  it("usa uma mensagem padrão quando o erro não tem corpo", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("falhou", { status: 502 })),
    );

    await expect(api.get("/eu")).rejects.toThrow("Não foi possível falar com o servidor. Tente de novo.");
  });

  it("identifica sessão expirada e extrai mensagens", () => {
    expect(naoAutenticado(new ErroDaApi(401, "x"))).toBe(true);
    expect(naoAutenticado(new ErroDaApi(403, "x"))).toBe(false);
    expect(naoAutenticado(new Error("x"))).toBe(false);
    expect(mensagemDe(new Error("Deu ruim"))).toBe("Deu ruim");
    expect(mensagemDe("texto solto")).toBe("Algo deu errado. Tente de novo.");
  });
});

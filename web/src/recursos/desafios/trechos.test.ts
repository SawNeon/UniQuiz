import { dividirEmTrechos } from "./trechos";

describe("dividirEmTrechos", () => {
  it("separa texto e lacunas na ordem em que aparecem", () => {
    expect(dividirEmTrechos("Use ferramentas {{1}} pela {{2}}.")).toEqual([
      { tipo: "texto", valor: "Use ferramentas " },
      { tipo: "lacuna", lacunaId: 1 },
      { tipo: "texto", valor: " pela " },
      { tipo: "lacuna", lacunaId: 2 },
      { tipo: "texto", valor: "." },
    ]);
  });

  it("aceita lacuna no começo e texto sem lacunas", () => {
    expect(dividirEmTrechos("{{3}} ok")).toEqual([
      { tipo: "lacuna", lacunaId: 3 },
      { tipo: "texto", valor: " ok" },
    ]);
    expect(dividirEmTrechos("sem lacunas")).toEqual([{ tipo: "texto", valor: "sem lacunas" }]);
  });
});

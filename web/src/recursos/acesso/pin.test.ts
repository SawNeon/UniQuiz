import { pinCompleto, pinDigitado, TAMANHO_DO_PIN } from "./pin";

describe("pin", () => {
  it("tem 3 números", () => {
    expect(TAMANHO_DO_PIN).toBe(3);
  });

  it("mantém só os números digitados, até o tamanho do PIN", () => {
    expect(pinDigitado("a1b2c3d4")).toBe("123");
    expect(pinDigitado("  9 8 ")).toBe("98");
    expect(pinDigitado("abc")).toBe("");
    expect(pinDigitado("")).toBe("");
  });

  it("só considera completo com 3 números", () => {
    expect(pinCompleto("123")).toBe(true);
    expect(pinCompleto("12")).toBe(false);
    expect(pinCompleto("")).toBe(false);
  });
});

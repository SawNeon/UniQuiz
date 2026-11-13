import { forcaDaSenha } from "./forcaDaSenha";

describe("forcaDaSenha", () => {
  it.each([
    ["curta", "maria", "Curta demais"],
    ["aaaaaaaaaaaa", "maria", "Fácil de adivinhar"],
    ["123456789012", "maria", "Fácil de adivinhar"],
    ["Senha@123456", "maria", "Fácil de adivinhar"],
    ["a maria quer cafe", "maria", "Fácil de adivinhar"],
    ["girassol azul", "maria", "Razoável"],
    ["girassol azul amarelo", "maria", "Forte"],
    ["café forte antes da reunião", "maria", "Forte"],
    ["Xp9!kd2Lq0#z", "maria", "Boa"],
  ])("avalia %s como %s", (senha, usuario, rotulo) => {
    expect(forcaDaSenha(senha, usuario).rotulo).toBe(rotulo);
  });
});

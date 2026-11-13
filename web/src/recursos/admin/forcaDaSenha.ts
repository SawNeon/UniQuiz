export const TAMANHO_MINIMO_DA_SENHA = 12;
export const TAMANHO_MAXIMO_DA_SENHA = 128;

export interface ForcaDaSenha {
  nivel: 0 | 1 | 2 | 3 | 4;
  rotulo: string;
}

const ROTULOS = ["Curta demais", "Fácil de adivinhar", "Razoável", "Boa", "Forte"] as const;

const PALAVRAS_PREVISIVEIS = new Set([
  "senha",
  "password",
  "admin",
  "administrador",
  "unimed",
  "uniquiz",
  "qwerty",
  "teste",
  "brasil",
  "mudar",
  "trocar",
  "acesso",
  "segredo",
  "usuario",
  "login",
]);

const SEQUENCIAS = [
  "01234567890123456789",
  "98765432109876543210",
  "abcdefghijklmnopqrstuvwxyz",
  "qwertyuiopasdfghjklzxcvbnm",
];

function simplificar(texto: string): string {
  return texto.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
}

function repeteUmPadraoCurto(texto: string): boolean {
  for (let tamanho = 1; tamanho <= 4; tamanho++) {
    if (texto.length % tamanho === 0 && texto.slice(0, tamanho).repeat(texto.length / tamanho) === texto) {
      return true;
    }
  }
  return false;
}

function previsivel(senha: string, usuario?: string): boolean {
  const simples = simplificar(senha);
  const soLetras = simples.replace(/[^\p{L}]/gu, "");
  const usuarioSimples = usuario ? simplificar(usuario.trim()) : "";
  return (
    repeteUmPadraoCurto(simples) ||
    SEQUENCIAS.some((sequencia) => sequencia.includes(simples)) ||
    PALAVRAS_PREVISIVEIS.has(soLetras) ||
    (usuarioSimples.length >= 3 && simples.includes(usuarioSimples))
  );
}

export function forcaDaSenha(senha: string, usuario?: string): ForcaDaSenha {
  const nivel = calcularNivel(senha, usuario);
  return { nivel, rotulo: ROTULOS[nivel] };
}

function calcularNivel(senha: string, usuario?: string): ForcaDaSenha["nivel"] {
  if (senha.length < TAMANHO_MINIMO_DA_SENHA) {
    return 0;
  }
  if (previsivel(senha, usuario)) {
    return 1;
  }
  const tiposDeCaractere = [/\p{Ll}/u, /\p{Lu}/u, /\d/, /[^\p{L}\d]/u].filter((tipo) =>
    tipo.test(senha),
  ).length;
  const palavras = senha.trim().split(/\s+/).length;
  let pontos = 0;
  if (senha.length >= 16) {
    pontos++;
  }
  if (senha.length >= 20) {
    pontos++;
  }
  if (tiposDeCaractere >= 3 || palavras >= 4) {
    pontos++;
  }
  return pontos >= 2 ? 4 : pontos === 1 ? 3 : 2;
}

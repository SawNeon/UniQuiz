import type { PorNivel } from "../../api/tipos";

export const MAXIMO_POR_NIVEL = 30;

export const NIVEIS = [
  { chave: "facil", rotulo: "Fáceis", singular: "fácil" },
  { chave: "medio", rotulo: "Médios", singular: "médio" },
  { chave: "dificil", rotulo: "Difíceis", singular: "difícil" },
] as const;

export function totalDeDesafios(quantidades: PorNivel): number {
  return quantidades.facil + quantidades.medio + quantidades.dificil;
}

export function quantidadeDigitada(texto: string): number {
  const numero = Math.trunc(Number(texto));
  if (!Number.isFinite(numero) || numero < 0) {
    return 0;
  }
  return Math.min(numero, MAXIMO_POR_NIVEL);
}

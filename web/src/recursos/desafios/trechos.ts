export type Trecho = { tipo: "texto"; valor: string } | { tipo: "lacuna"; lacunaId: number };

const MARCADOR = /\{\{(\d+)\}\}/g;

export function dividirEmTrechos(texto: string): Trecho[] {
  const trechos: Trecho[] = [];
  let inicio = 0;
  for (const encontrado of texto.matchAll(MARCADOR)) {
    const posicao = encontrado.index ?? 0;
    if (posicao > inicio) {
      trechos.push({ tipo: "texto", valor: texto.slice(inicio, posicao) });
    }
    trechos.push({ tipo: "lacuna", lacunaId: Number(encontrado[1]) });
    inicio = posicao + encontrado[0].length;
  }
  if (inicio < texto.length) {
    trechos.push({ tipo: "texto", valor: texto.slice(inicio) });
  }
  return trechos;
}

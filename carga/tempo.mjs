export function pausar(ms) {
  return new Promise((resolver) => setTimeout(resolver, ms));
}

export function sortearEntre(minimoMs, maximoMs) {
  return minimoMs + Math.random() * (maximoMs - minimoMs);
}

export async function pensar(intervaloMs) {
  if (intervaloMs === null || intervaloMs[1] === 0) {
    return;
  }
  await pausar(sortearEntre(intervaloMs[0], intervaloMs[1]));
}

export async function emParalelo(itens, concorrencia, trabalho) {
  const fila = [...itens];
  const operarios = Array.from({ length: Math.min(concorrencia, fila.length) }, async () => {
    while (fila.length > 0) {
      await trabalho(fila.shift());
    }
  });
  await Promise.all(operarios);
}

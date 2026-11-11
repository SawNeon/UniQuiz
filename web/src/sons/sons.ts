import { useSyncExternalStore } from "react";

export type Som = "acerto" | "erro" | "conclusao" | "trofeu";

const CHAVE = "uniquiz:som-ligado";

const notas: Record<Som, { frequencia: number; duracao: number }[]> = {
  acerto: [
    { frequencia: 660, duracao: 0.09 },
    { frequencia: 880, duracao: 0.14 },
  ],
  erro: [
    { frequencia: 330, duracao: 0.12 },
    { frequencia: 262, duracao: 0.18 },
  ],
  conclusao: [
    { frequencia: 523, duracao: 0.1 },
    { frequencia: 659, duracao: 0.1 },
    { frequencia: 784, duracao: 0.22 },
  ],
  trofeu: [
    { frequencia: 784, duracao: 0.1 },
    { frequencia: 988, duracao: 0.1 },
    { frequencia: 1175, duracao: 0.26 },
  ],
};

const ouvintes = new Set<() => void>();
let contexto: AudioContext | null = null;

function lerPreferencia(): boolean {
  try {
    return localStorage.getItem(CHAVE) === "sim";
  } catch {
    return false;
  }
}

let ligado = lerPreferencia();

export function alternarSom(): void {
  ligado = !ligado;
  try {
    localStorage.setItem(CHAVE, ligado ? "sim" : "nao");
  } catch {
    // Sem armazenamento local, a escolha vale só nesta aba.
  }
  ouvintes.forEach((ouvinte) => ouvinte());
}

export function useSomLigado(): boolean {
  return useSyncExternalStore(
    (ouvinte) => {
      ouvintes.add(ouvinte);
      return () => ouvintes.delete(ouvinte);
    },
    () => ligado,
  );
}

export function tocar(som: Som): void {
  if (!ligado || typeof AudioContext === "undefined") {
    return;
  }
  contexto ??= new AudioContext();
  let inicio = contexto.currentTime;
  for (const { frequencia, duracao } of notas[som]) {
    const oscilador = contexto.createOscillator();
    const volume = contexto.createGain();
    oscilador.type = "sine";
    oscilador.frequency.value = frequencia;
    volume.gain.setValueAtTime(0.12, inicio);
    volume.gain.exponentialRampToValueAtTime(0.001, inicio + duracao);
    oscilador.connect(volume).connect(contexto.destination);
    oscilador.start(inicio);
    oscilador.stop(inicio + duracao);
    inicio += duracao;
  }
}

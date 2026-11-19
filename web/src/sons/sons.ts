import { useSyncExternalStore } from "react";

export type Som =
  | "clique"
  | "selecionar"
  | "navegar"
  | "abrir"
  | "negado"
  | "pin"
  | "acerto"
  | "erro"
  | "conclusao"
  | "trofeu";

interface Nota {
  frequencia: number;
  inicio: number;
  duracao: number;
  tipo?: OscillatorType;
  volume?: number;
  ate?: number;
}

const CHAVE = "uniquiz:som-ligado";
const VOLUME_PADRAO = 0.07;
const VIBRACAO_EM_MS = 30;

const notas: Record<Som, Nota[]> = {
  clique: [{ frequencia: 740, inicio: 0, duracao: 0.05, tipo: "triangle", volume: 0.05 }],
  selecionar: [
    { frequencia: 523, inicio: 0, duracao: 0.06, tipo: "triangle", volume: 0.06 },
    { frequencia: 659, inicio: 0.05, duracao: 0.09, tipo: "triangle", volume: 0.06 },
  ],
  navegar: [
    { frequencia: 440, inicio: 0, duracao: 0.06, volume: 0.06 },
    { frequencia: 587, inicio: 0.05, duracao: 0.1, volume: 0.05 },
  ],
  abrir: [
    { frequencia: 392, inicio: 0, duracao: 0.08, volume: 0.06 },
    { frequencia: 523, inicio: 0.07, duracao: 0.08, volume: 0.06 },
    { frequencia: 659, inicio: 0.14, duracao: 0.14, volume: 0.06 },
  ],
  negado: [{ frequencia: 180, inicio: 0, duracao: 0.12, tipo: "triangle", volume: 0.08, ate: 150 }],
  pin: [{ frequencia: 880, inicio: 0, duracao: 0.035, tipo: "triangle", volume: 0.04 }],
  acerto: [
    { frequencia: 660, inicio: 0, duracao: 0.1, volume: 0.08 },
    { frequencia: 880, inicio: 0.09, duracao: 0.16, volume: 0.08 },
    { frequencia: 1320, inicio: 0.2, duracao: 0.2, volume: 0.03 },
  ],
  erro: [
    { frequencia: 294, inicio: 0, duracao: 0.14, tipo: "triangle", volume: 0.09, ate: 262 },
    { frequencia: 220, inicio: 0.13, duracao: 0.22, tipo: "triangle", volume: 0.09, ate: 196 },
  ],
  conclusao: [
    { frequencia: 523, inicio: 0, duracao: 0.12 },
    { frequencia: 659, inicio: 0.1, duracao: 0.12 },
    { frequencia: 784, inicio: 0.2, duracao: 0.12 },
    { frequencia: 1047, inicio: 0.3, duracao: 0.34 },
  ],
  trofeu: [
    { frequencia: 784, inicio: 0, duracao: 0.12 },
    { frequencia: 988, inicio: 0.09, duracao: 0.12 },
    { frequencia: 1175, inicio: 0.18, duracao: 0.12 },
    { frequencia: 1568, inicio: 0.27, duracao: 0.42 },
    { frequencia: 2349, inicio: 0.3, duracao: 0.3, volume: 0.025 },
  ],
};

const vibracoes: Partial<Record<Som, number>> = { acerto: VIBRACAO_EM_MS, erro: VIBRACAO_EM_MS };

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

function tocarNota(audio: AudioContext, base: number, nota: Nota): void {
  const inicio = base + nota.inicio;
  const fim = inicio + nota.duracao;
  const oscilador = audio.createOscillator();
  const volume = audio.createGain();
  oscilador.type = nota.tipo ?? "sine";
  oscilador.frequency.setValueAtTime(nota.frequencia, inicio);
  if (nota.ate) {
    oscilador.frequency.exponentialRampToValueAtTime(nota.ate, fim);
  }
  volume.gain.setValueAtTime(0.0001, inicio);
  volume.gain.exponentialRampToValueAtTime(nota.volume ?? VOLUME_PADRAO, inicio + 0.012);
  volume.gain.exponentialRampToValueAtTime(0.0001, fim);
  oscilador.connect(volume).connect(audio.destination);
  oscilador.start(inicio);
  oscilador.stop(fim + 0.02);
}

export function tocar(som: Som): void {
  if (!ligado) {
    return;
  }
  const vibracao = vibracoes[som];
  if (vibracao && typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
    navigator.vibrate(vibracao);
  }
  if (typeof AudioContext === "undefined") {
    return;
  }
  contexto ??= new AudioContext();
  if (contexto.state === "suspended") {
    void contexto.resume();
  }
  const base = contexto.currentTime + 0.01;
  for (const nota of notas[som]) {
    tocarNota(contexto, base, nota);
  }
}

import { create } from "zustand";
import type { Correcao, DesafioPublico, RespostaDoColaborador, TentativaIniciada } from "../../api/tipos";

interface EstadoDaLicao {
  tentativa: TentativaIniciada | null;
  fila: number[];
  resposta: RespostaDoColaborador | null;
  correcao: Correcao | null;
  pendentes: number[];
  segundaRodada: boolean;
  respondidos: number;
  iniciar: (tentativa: TentativaIniciada) => void;
  responder: (resposta: RespostaDoColaborador) => void;
  registrarCorrecao: (correcao: Correcao) => void;
  avancar: () => "continuar" | "concluir";
  limpar: () => void;
}

const vazio = {
  tentativa: null,
  fila: [],
  resposta: null,
  correcao: null,
  pendentes: [],
  segundaRodada: false,
  respondidos: 0,
};

export const useEstadoDaLicao = create<EstadoDaLicao>()((definir, obter) => ({
  ...vazio,

  iniciar: (tentativa) =>
    definir({ ...vazio, tentativa, fila: tentativa.desafios.map((desafio) => desafio.id) }),

  responder: (resposta) => {
    if (!obter().correcao) {
      definir({ resposta });
    }
  },

  registrarCorrecao: (correcao) =>
    definir((estado) => ({
      correcao,
      respondidos: estado.respondidos + 1,
      pendentes: estado.segundaRodada ? estado.pendentes : correcao.desafiosParaSegundaChance,
    })),

  avancar: () => {
    const estado = obter();
    let fila = estado.fila.slice(1);
    let segundaRodada = estado.segundaRodada;
    if (fila.length === 0 && !segundaRodada && estado.pendentes.length > 0) {
      fila = estado.pendentes;
      segundaRodada = true;
    }
    definir({ fila, segundaRodada, resposta: null, correcao: null });
    return fila.length === 0 ? "concluir" : "continuar";
  },

  limpar: () => definir(vazio),
}));

export function desafioAtual(estado: Pick<EstadoDaLicao, "tentativa" | "fila">): DesafioPublico | null {
  const id = estado.fila[0];
  return estado.tentativa?.desafios.find((desafio) => desafio.id === id) ?? null;
}

export function totalDePassos(estado: Pick<EstadoDaLicao, "tentativa" | "segundaRodada" | "pendentes">): number {
  const base = estado.tentativa?.desafios.length ?? 0;
  return estado.segundaRodada ? base + estado.pendentes.length : base;
}

import { motion, useReducedMotion } from "framer-motion";
import { useMemo } from "react";

const CORES = ["#b1d34b", "#f47920", "#00995d", "#a4d8de"];
const PECAS = 28;

interface Peca {
  esquerda: number;
  cor: string;
  atraso: number;
  duracao: number;
  giro: number;
}

function gerarPecas(): Peca[] {
  return Array.from({ length: PECAS }, (_, indice) => ({
    esquerda: Math.random() * 100,
    cor: CORES[indice % CORES.length] ?? "#b1d34b",
    atraso: Math.random() * 0.6,
    duracao: 1.8 + Math.random() * 1.2,
    giro: 240 + Math.random() * 360,
  }));
}

export function Confete() {
  const reduzir = useReducedMotion();
  const pecas = useMemo(gerarPecas, []);

  if (reduzir) {
    return null;
  }
  return (
    <div aria-hidden="true" className="pointer-events-none fixed inset-x-0 top-0 z-10 h-dvh overflow-hidden">
      {pecas.map((peca, indice) => (
        <motion.i
          key={indice}
          className="absolute top-0 block h-3.5 w-2 rounded-sm"
          style={{ left: `${peca.esquerda}%`, backgroundColor: peca.cor }}
          initial={{ y: -20, opacity: 1, rotate: 0 }}
          animate={{ y: "105dvh", opacity: [1, 1, 0], rotate: peca.giro }}
          transition={{ duration: peca.duracao, delay: peca.atraso, ease: "easeIn" }}
        />
      ))}
    </div>
  );
}

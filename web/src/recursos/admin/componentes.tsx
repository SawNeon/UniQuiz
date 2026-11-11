import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from "react";
import { forwardRef } from "react";
import type { StatusPublicacao } from "../../api/tipos";

const ROTULOS_DE_STATUS: Record<StatusPublicacao, string> = {
  RASCUNHO: "Rascunho",
  PUBLICADO: "Publicado",
  ARQUIVADO: "Arquivado",
};

const CORES_DE_STATUS: Record<StatusPublicacao, string> = {
  RASCUNHO: "bg-aviso",
  PUBLICADO: "bg-acerto-suave",
  ARQUIVADO: "bg-neutro",
};

export function SeloDeStatus({ status }: { status: StatusPublicacao }) {
  return (
    <span className={`rounded-lg px-2 py-0.5 text-sm font-bold ${CORES_DE_STATUS[status]}`}>
      {ROTULOS_DE_STATUS[status]}
    </span>
  );
}

export function SeletorDeStatus({
  status,
  aoMudar,
  rotulo,
}: {
  status: StatusPublicacao;
  aoMudar: (status: StatusPublicacao) => void;
  rotulo: string;
}) {
  return (
    <select
      aria-label={rotulo}
      value={status}
      onChange={(evento) => aoMudar(evento.target.value as StatusPublicacao)}
      className="min-h-11 rounded-xl border-2 border-neutro bg-white px-2 font-bold"
    >
      {Object.entries(ROTULOS_DE_STATUS).map(([valor, texto]) => (
        <option key={valor} value={valor}>
          {texto}
        </option>
      ))}
    </select>
  );
}

export function BotoesDeOrdem<T extends { id: number }>({
  itens,
  indice,
  aoReordenar,
  nome,
}: {
  itens: T[];
  indice: number;
  aoReordenar: (ids: number[]) => void;
  nome: string;
}) {
  function mover(passo: -1 | 1) {
    const ids = itens.map((item) => item.id);
    const destino = indice + passo;
    const atual = ids[indice];
    const vizinho = ids[destino];
    if (atual === undefined || vizinho === undefined) {
      return;
    }
    ids[indice] = vizinho;
    ids[destino] = atual;
    aoReordenar(ids);
  }

  return (
    <span className="flex gap-1">
      <button
        type="button"
        aria-label={`Subir ${nome}`}
        disabled={indice === 0}
        onClick={() => mover(-1)}
        className="size-11 rounded-xl bg-neutro font-extrabold disabled:opacity-40"
      >
        ↑
      </button>
      <button
        type="button"
        aria-label={`Descer ${nome}`}
        disabled={indice === itens.length - 1}
        onClick={() => mover(1)}
        className="size-11 rounded-xl bg-neutro font-extrabold disabled:opacity-40"
      >
        ↓
      </button>
    </span>
  );
}

export function Campo({ rotulo, erro, children }: { rotulo: string; erro?: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="font-extrabold">{rotulo}</span>
      {children}
      {erro && <span className="text-sm font-bold">{erro}</span>}
    </label>
  );
}

const ESTILO_DO_CAMPO = "min-h-11 rounded-xl border-2 border-neutro bg-white px-3 py-2";

export const Entrada = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Entrada(
  { className = "", ...resto },
  ref,
) {
  return <input ref={ref} className={`${ESTILO_DO_CAMPO} ${className}`} {...resto} />;
});

export const AreaDeTexto = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function AreaDeTexto({ className = "", ...resto }, ref) {
    return <textarea ref={ref} className={`${ESTILO_DO_CAMPO} ${className}`} {...resto} />;
  },
);

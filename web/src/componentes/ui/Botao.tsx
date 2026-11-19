import type { ButtonHTMLAttributes, MouseEvent } from "react";
import { tocar } from "../../sons/sons";

type Variante = "principal" | "secundario" | "discreto";

interface PropsBotao extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  larguraTotal?: boolean;
}

const estilos: Record<Variante, string> = {
  principal:
    "bg-primaria text-white shadow-[0_4px_0_#006b41] active:translate-y-[3px] active:shadow-[0_1px_0_#006b41] hover:bg-[#00995d]/90 disabled:bg-[#00995d]/40 disabled:shadow-none",
  secundario:
    "bg-neutro text-texto shadow-[0_4px_0_#d6cabb] active:translate-y-[3px] active:shadow-[0_1px_0_#d6cabb] hover:bg-[#ece3d9]/80 disabled:opacity-60 disabled:shadow-none",
  discreto: "bg-transparent text-texto underline-offset-4 hover:underline disabled:opacity-60",
};

export function Botao({
  variante = "principal",
  larguraTotal = false,
  className = "",
  type = "button",
  onClick,
  ...resto
}: PropsBotao) {
  function aoClicar(evento: MouseEvent<HTMLButtonElement>) {
    tocar("clique");
    onClick?.(evento);
  }

  return (
    <button
      type={type}
      className={[
        "min-h-11 rounded-padrao px-5 text-lg font-extrabold transition-[transform,box-shadow,background-color] duration-100 disabled:cursor-not-allowed motion-reduce:transition-none",
        estilos[variante],
        larguraTotal ? "w-full" : "",
        className,
      ].join(" ")}
      onClick={aoClicar}
      {...resto}
    />
  );
}

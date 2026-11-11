import type { ButtonHTMLAttributes } from "react";

type Variante = "principal" | "secundario" | "discreto";

interface PropsBotao extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  larguraTotal?: boolean;
}

const estilos: Record<Variante, string> = {
  principal: "bg-primaria text-white hover:bg-[#00995d]/80 disabled:bg-[#00995d]/40",
  secundario: "bg-neutro text-texto hover:bg-[#ece3d9]/80 disabled:opacity-60",
  discreto: "bg-transparent text-texto underline-offset-4 hover:underline disabled:opacity-60",
};

export function Botao({ variante = "principal", larguraTotal = false, className = "", type = "button", ...resto }: PropsBotao) {
  return (
    <button
      type={type}
      className={[
        "min-h-11 rounded-padrao px-5 text-lg font-extrabold transition-colors disabled:cursor-not-allowed",
        estilos[variante],
        larguraTotal ? "w-full" : "",
        className,
      ].join(" ")}
      {...resto}
    />
  );
}

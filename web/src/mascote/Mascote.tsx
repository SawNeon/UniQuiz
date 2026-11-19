import acenando from "./arte/tiuv-02-acenando.svg";
import offline from "./arte/tiuv-06-offline.svg";
import ops from "./arte/tiuv-05-ops.svg";
import padrao from "./arte/tiuv-01-padrao.svg";
import pensando from "./arte/tiuv-04-pensando.svg";
import sucesso from "./arte/tiuv-03-sucesso.svg";
import sucessoComContorno from "./arte/tiuv-03-sucesso-contorno.svg";

export type EstadoDoMascote = "padrao" | "acenando" | "sucesso" | "pensando" | "ops" | "offline";

const imagens: Record<EstadoDoMascote, string> = {
  padrao,
  acenando,
  sucesso,
  pensando,
  ops,
  offline,
};

const rotulos: Record<EstadoDoMascote, string> = {
  padrao: "Tiuv, o enfermeiro do TI",
  acenando: "Tiuv acenando",
  sucesso: "Tiuv comemorando",
  pensando: "Tiuv pensando",
  ops: "Tiuv preocupado",
  offline: "Tiuv em manutenção",
};

const TAMANHO_MINIMO = 64;

interface PropsMascote {
  estado?: EstadoDoMascote;
  tamanho?: number;
  contorno?: boolean;
  className?: string;
}

export function Mascote({ estado = "padrao", tamanho = 96, contorno = false, className = "" }: PropsMascote) {
  const origem = contorno && estado === "sucesso" ? sucessoComContorno : imagens[estado];
  return (
    <img
      src={origem}
      alt={rotulos[estado]}
      width={Math.max(tamanho, TAMANHO_MINIMO)}
      height={Math.max(tamanho, TAMANHO_MINIMO)}
      className={["select-none", className].join(" ")}
      draggable={false}
    />
  );
}

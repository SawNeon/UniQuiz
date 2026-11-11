export type EstadoDoMascote = "neutro" | "feliz" | "triste" | "comemorando";

const rotulos: Record<EstadoDoMascote, string> = {
  neutro: "Bit, o mascote da TI",
  feliz: "Bit feliz",
  triste: "Bit triste",
  comemorando: "Bit comemorando",
};

function Olhos({ estado }: { estado: EstadoDoMascote }) {
  if (estado === "feliz" || estado === "comemorando") {
    return (
      <>
        <path d="M41 43 Q47 36 53 43" />
        <path d="M67 43 Q73 36 79 43" />
      </>
    );
  }
  return (
    <>
      <circle cx="47" cy="43" r="5" fill="#004e4c" stroke="none" />
      <circle cx="73" cy="43" r="5" fill="#004e4c" stroke="none" />
      {estado === "triste" && (
        <>
          <path d="M40 34 L52 37" strokeWidth={3} />
          <path d="M80 34 L68 37" strokeWidth={3} />
        </>
      )}
    </>
  );
}

function Boca({ estado }: { estado: EstadoDoMascote }) {
  switch (estado) {
    case "feliz":
      return <path d="M48 54 Q60 64 72 54" />;
    case "triste":
      return <path d="M50 61 Q60 53 70 61" />;
    case "comemorando":
      return <path d="M46 51 H74 Q72 65 60 65 Q48 65 46 51z" fill="#004e4c" />;
    default:
      return <path d="M50 57 H70" />;
  }
}

export function Mascote({ estado = "neutro", tamanho = 96 }: { estado?: EstadoDoMascote; tamanho?: number }) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 120 120" role="img" aria-label={rotulos[estado]}>
      {estado === "comemorando" && (
        <>
          <rect x="4" y="14" width="9" height="4" rx="2" fill="#f47920" transform="rotate(-30 8 16)" />
          <rect x="104" y="8" width="9" height="4" rx="2" fill="#00995d" transform="rotate(25 108 10)" />
          <circle cx="110" cy="70" r="3" fill="#f47920" />
          <circle cx="8" cy="62" r="3" fill="#b1d34b" />
        </>
      )}
      <rect x="14" y="10" width="92" height="72" rx="18" fill="#00995d" />
      <rect x="24" y="20" width="72" height="52" rx="12" fill={estado === "neutro" ? "#cde3bb" : "#ffffff"} />
      <g fill="none" stroke="#004e4c" strokeWidth={4} strokeLinecap="round">
        <Olhos estado={estado} />
        <Boca estado={estado} />
      </g>
      <rect x="52" y="82" width="16" height="12" fill="#004e4c" />
      <rect x="34" y="92" width="52" height="10" rx="5" fill="#004e4c" />
    </svg>
  );
}

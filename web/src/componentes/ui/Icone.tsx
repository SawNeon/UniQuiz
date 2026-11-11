import type { SVGProps } from "react";

const desenhos = {
  caminho: (
    <>
      <circle cx="6" cy="19" r="2" />
      <circle cx="18" cy="5" r="2" />
      <path d="M8 19h7a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h7" />
    </>
  ),
  trofeu: (
    <>
      <path d="M8 4h8v5a4 4 0 0 1-8 0z" />
      <path d="M8 6H5a3 3 0 0 0 3 4" />
      <path d="M16 6h3a3 3 0 0 1-3 4" />
      <path d="M12 13v4" />
      <path d="M9 20h6" />
    </>
  ),
  perfil: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M5 20a7 7 0 0 1 14 0" />
    </>
  ),
  cadeado: (
    <>
      <rect x="5" y="11" width="14" height="10" rx="3" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </>
  ),
  certo: <path d="M5 12l5 5 9-10" />,
  errado: (
    <>
      <path d="M6 6l12 12" />
      <path d="M18 6L6 18" />
    </>
  ),
  livro: (
    <>
      <path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z" />
      <path d="M4 21V5" />
      <path d="M9 7h6" />
    </>
  ),
  certificado: (
    <>
      <rect x="3" y="4" width="18" height="12" rx="3" />
      <circle cx="12" cy="17" r="3" />
      <path d="M10.5 19.5L9.5 22l2.5-1 2.5 1-1-2.5" />
    </>
  ),
  escudo: (
    <>
      <path d="M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6z" />
      <path d="M9 12l2 2 4-4" />
    </>
  ),
  chama: <path d="M12 2c3 4 7 6.5 7 11.5A7 7 0 0 1 5 13.5c0-3 1.8-4.6 3-6.5.8 2 2 3 4 3.2C12 7.5 11.5 4.5 12 2z" />,
  estrela: <path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z" />,
  passo: (
    <>
      <path d="M5 19h14" />
      <path d="M8 19v-5h4v-4h4V5" />
    </>
  ),
  rede: (
    <>
      <circle cx="6" cy="6" r="2.5" />
      <circle cx="18" cy="6" r="2.5" />
      <circle cx="12" cy="18" r="2.5" />
      <path d="M8.5 6h7" />
      <path d="M7.3 8.2l3.4 7.6" />
      <path d="M16.7 8.2l-3.4 7.6" />
    </>
  ),
  somLigado: (
    <>
      <path d="M4 9h4l5-4v14l-5-4H4z" />
      <path d="M17 9a4 4 0 0 1 0 6" />
      <path d="M19.5 6.5a8 8 0 0 1 0 11" />
    </>
  ),
  somDesligado: (
    <>
      <path d="M4 9h4l5-4v14l-5-4H4z" />
      <path d="M17 9l4 6" />
      <path d="M21 9l-4 6" />
    </>
  ),
  fechar: (
    <>
      <path d="M6 6l12 12" />
      <path d="M18 6L6 18" />
    </>
  ),
  voltar: <path d="M15 5l-7 7 7 7" />,
  baixar: (
    <>
      <path d="M12 4v11" />
      <path d="M7 10l5 5 5-5" />
      <path d="M5 20h14" />
    </>
  ),
  coroa: <path d="M4 18h16l-1.5-9-4 3.5L12 6l-2.5 6.5-4-3.5z" />,
};

export type NomeDoIcone = keyof typeof desenhos;

interface PropsIcone extends Omit<SVGProps<SVGSVGElement>, "name"> {
  nome: NomeDoIcone;
  tamanho?: number;
}

export function Icone({ nome, tamanho = 24, strokeWidth = 2, ...resto }: PropsIcone) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...resto}
    >
      {desenhos[nome]}
    </svg>
  );
}

export function iconeDoTrofeu(icone: string): NomeDoIcone {
  return icone in desenhos ? (icone as NomeDoIcone) : "trofeu";
}

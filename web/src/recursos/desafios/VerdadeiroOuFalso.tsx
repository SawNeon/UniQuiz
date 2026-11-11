import type { RespostaVerdadeiroOuFalso } from "../../api/tipos";
import type { PropsDesafio } from "./contrato";

const OPCOES = [
  { valor: true, rotulo: "Verdadeiro" },
  { valor: false, rotulo: "Falso" },
];

export function VerdadeiroOuFalso({
  desafio,
  resposta,
  aoResponder,
  desabilitado,
}: PropsDesafio<"VERDADEIRO_OU_FALSO", RespostaVerdadeiroOuFalso>) {
  return (
    <div className="flex flex-col gap-5">
      <p className="rounded-[20px] bg-neutro p-5 font-slab text-xl leading-relaxed">
        {desafio.conteudo.afirmacao}
      </p>
      <div role="radiogroup" aria-label="Escolha verdadeiro ou falso" className="grid grid-cols-2 gap-3">
        {OPCOES.map((opcao) => (
          <button
            key={opcao.rotulo}
            type="button"
            role="radio"
            aria-checked={resposta?.valor === opcao.valor}
            disabled={desabilitado}
            onClick={() => aoResponder({ valor: opcao.valor })}
            className={[
              "min-h-14 rounded-padrao border-2 text-lg font-extrabold",
              resposta?.valor === opcao.valor
                ? "border-texto bg-acerto-suave"
                : "border-neutro hover:bg-neutro",
            ].join(" ")}
          >
            {opcao.rotulo}
          </button>
        ))}
      </div>
    </div>
  );
}

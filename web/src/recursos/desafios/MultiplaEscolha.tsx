import type { KeyboardEvent } from "react";
import type { RespostaMultiplaEscolha } from "../../api/tipos";
import type { PropsDesafio } from "./contrato";

export function MultiplaEscolha({
  desafio,
  resposta,
  aoResponder,
  correcao,
  desabilitado,
}: PropsDesafio<"MULTIPLA_ESCOLHA", RespostaMultiplaEscolha>) {
  const alternativas = desafio.conteudo.alternativas;

  function navegarPorSetas(evento: KeyboardEvent<HTMLDivElement>) {
    if (desabilitado || (evento.key !== "ArrowDown" && evento.key !== "ArrowUp")) {
      return;
    }
    evento.preventDefault();
    const atual = alternativas.findIndex((alternativa) => alternativa.id === resposta?.alternativaId);
    const passo = evento.key === "ArrowDown" ? 1 : -1;
    const proxima = alternativas[(atual + passo + alternativas.length) % alternativas.length];
    if (proxima) {
      aoResponder({ alternativaId: proxima.id });
    }
  }

  return (
    <div role="radiogroup" aria-label={desafio.enunciado} onKeyDown={navegarPorSetas} className="flex flex-col gap-3">
      {alternativas.map((alternativa) => {
        const escolhida = resposta?.alternativaId === alternativa.id;
        const destaque =
          escolhida && correcao ? (correcao.correta ? "border-primaria bg-acerto-suave" : "border-texto bg-erro-suave") : "";
        return (
          <button
            key={alternativa.id}
            type="button"
            role="radio"
            aria-checked={escolhida}
            tabIndex={escolhida || (!resposta && alternativa === alternativas[0]) ? 0 : -1}
            disabled={desabilitado}
            onClick={() => aoResponder({ alternativaId: alternativa.id })}
            className={[
              "min-h-14 rounded-padrao border-2 px-4 py-3 text-left text-lg font-bold transition-colors",
              escolhida ? "border-texto" : "border-neutro hover:bg-neutro",
              destaque,
            ].join(" ")}
          >
            {alternativa.texto}
          </button>
        );
      })}
    </div>
  );
}

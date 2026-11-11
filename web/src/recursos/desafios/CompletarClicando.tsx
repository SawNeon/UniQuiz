import { Fragment, useState } from "react";
import type { RespostaCompletarClicando } from "../../api/tipos";
import type { PropsDesafio } from "./contrato";
import { dividirEmTrechos } from "./trechos";

export function CompletarClicando({
  desafio,
  resposta,
  aoResponder,
  desabilitado,
}: PropsDesafio<"COMPLETAR_CLICANDO", RespostaCompletarClicando>) {
  const { texto, lacunas } = desafio.conteudo;
  const preenchidas = resposta?.lacunas ?? {};
  const primeiraVazia = lacunas.find((lacuna) => !preenchidas[String(lacuna.id)])?.id;
  const [selecionada, setSelecionada] = useState<number | null>(null);
  const ativa = selecionada ?? primeiraVazia ?? lacunas[0]?.id ?? null;
  const bancoDaAtiva = lacunas.find((lacuna) => lacuna.id === ativa)?.palavras ?? [];

  function preencher(palavra: string) {
    if (ativa === null) {
      return;
    }
    aoResponder({ lacunas: { ...preenchidas, [String(ativa)]: palavra } });
    setSelecionada(null);
  }

  function esvaziar(lacunaId: number) {
    const restantes = { ...preenchidas };
    delete restantes[String(lacunaId)];
    aoResponder({ lacunas: restantes });
    setSelecionada(lacunaId);
  }

  return (
    <div className="flex flex-col gap-6">
      <p className="rounded-[20px] bg-neutro p-5 font-slab text-xl leading-[1.9]">
        {dividirEmTrechos(texto).map((trecho, indice) => (
          <Fragment key={indice}>
            {trecho.tipo === "texto" ? (
              trecho.valor
            ) : (
              <button
                type="button"
                disabled={desabilitado}
                aria-label={
                  preenchidas[String(trecho.lacunaId)]
                    ? `Lacuna preenchida com ${preenchidas[String(trecho.lacunaId)]}. Toque para esvaziar.`
                    : "Lacuna vazia"
                }
                onClick={() =>
                  preenchidas[String(trecho.lacunaId)] ? esvaziar(trecho.lacunaId) : setSelecionada(trecho.lacunaId)
                }
                className={[
                  "mx-1 inline-flex min-h-10 min-w-24 items-center justify-center rounded-xl border-2 bg-white px-3 align-middle font-sans text-[17px] font-extrabold",
                  ativa === trecho.lacunaId && !desabilitado ? "border-primaria" : "border-texto",
                  preenchidas[String(trecho.lacunaId)] ? "" : "border-dashed",
                ].join(" ")}
              >
                {preenchidas[String(trecho.lacunaId)] ?? " "}
              </button>
            )}
          </Fragment>
        ))}
      </p>

      <div role="group" aria-label="Banco de palavras" className="flex flex-wrap gap-2.5">
        {bancoDaAtiva.map((palavra) => {
          const usada = Object.values(preenchidas).includes(palavra);
          return (
            <button
              key={palavra}
              type="button"
              disabled={desabilitado || usada}
              onClick={() => preencher(palavra)}
              className="min-h-12 rounded-[14px] border-2 border-neutro bg-white px-4 text-[17px] font-bold hover:bg-neutro disabled:border-dashed disabled:text-transparent"
            >
              {palavra}
            </button>
          );
        })}
      </div>
    </div>
  );
}

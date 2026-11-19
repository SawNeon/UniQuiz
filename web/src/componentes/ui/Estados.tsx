import { mensagemDe } from "../../api/cliente";
import { Mascote } from "../../mascote/Mascote";

export function Carregando({
  texto = "Carregando…",
  comMascote = false,
}: {
  texto?: string;
  comMascote?: boolean;
}) {
  return (
    <div role="status" className="flex flex-col items-center gap-3 p-6 text-center font-bold">
      {comMascote && (
        <Mascote estado="pensando" tamanho={120} className="animate-pulse motion-reduce:animate-none" />
      )}
      <p>{texto}</p>
    </div>
  );
}

export function MensagemDeErro({ erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo?: () => void }) {
  return (
    <div role="alert" className="m-4 rounded-padrao bg-erro-suave p-4">
      <p className="font-bold">{mensagemDe(erro)}</p>
      {aoTentarDeNovo && (
        <button type="button" onClick={aoTentarDeNovo} className="mt-2 min-h-11 font-extrabold underline">
          Tentar de novo
        </button>
      )}
    </div>
  );
}

import { mensagemDe } from "../../api/cliente";

export function Carregando({ texto = "Carregando…" }: { texto?: string }) {
  return (
    <p role="status" className="p-6 text-center font-bold">
      {texto}
    </p>
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

import type { ReactNode } from "react";
import { isRouteErrorResponse, useLocation, useNavigate, useRouteError } from "react-router";
import { Botao } from "../componentes/ui/Botao";
import { Mascote } from "../mascote/Mascote";

function recarregarPagina() {
  window.location.reload();
}

function Moldura({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col items-center justify-center gap-6 px-4 py-8 text-center">
      <Mascote estado="triste" tamanho={112} />
      <h1 className="text-3xl font-extrabold">{titulo}</h1>
      {children}
    </main>
  );
}

function BotaoParaOInicio({ variante }: { variante: "principal" | "secundario" }) {
  const navegar = useNavigate();
  return (
    <Botao variante={variante} larguraTotal onClick={() => void navegar("/", { replace: true })}>
      Ir para o início
    </Botao>
  );
}

export function TelaDePaginaNaoEncontrada() {
  const { pathname } = useLocation();
  return (
    <Moldura titulo="Não encontramos essa página">
      <p className="text-lg">
        O endereço <strong className="break-all">{pathname}</strong> não existe ou mudou de lugar.
      </p>
      <BotaoParaOInicio variante="principal" />
    </Moldura>
  );
}

export function TelaDeErroInesperado({ aoRecarregar = recarregarPagina }: { aoRecarregar?: () => void }) {
  return (
    <Moldura titulo="Algo deu errado">
      <p className="text-lg">
        Não foi possível abrir esta tela. Recarregar costuma resolver, principalmente se o app acabou de ser
        atualizado.
      </p>
      <div className="flex w-full flex-col gap-3">
        <Botao larguraTotal onClick={aoRecarregar}>
          Recarregar
        </Botao>
        <BotaoParaOInicio variante="secundario" />
      </div>
    </Moldura>
  );
}

export function ErroDaRota({ aoRecarregar }: { aoRecarregar?: () => void }) {
  const erro = useRouteError();
  if (isRouteErrorResponse(erro) && erro.status === 404) {
    return <TelaDePaginaNaoEncontrada />;
  }
  return <TelaDeErroInesperado aoRecarregar={aoRecarregar} />;
}

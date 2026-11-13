import { Navigate, NavLink, Outlet, useNavigate } from "react-router";
import { useAdminEu, useSairAdmin } from "../../api/admin";
import { naoAutenticado } from "../../api/cliente";
import type { ContaLogada } from "../../api/tipos";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { FormularioDeTrocaDeSenha } from "./FormularioDeTrocaDeSenha";
import { ContextoDaConta, PERFIS_DO_PAINEL, secoesPermitidas } from "./painel";

export function LayoutAdmin() {
  const eu = useAdminEu();
  const sair = useSairAdmin();
  const navegar = useNavigate();

  if (eu.isPending) {
    return <Carregando />;
  }
  if (naoAutenticado(eu.error)) {
    return <Navigate to="/admin/entrar" replace />;
  }
  if (eu.isError) {
    return <MensagemDeErro erro={eu.error} aoTentarDeNovo={() => void eu.refetch()} />;
  }

  const conta = eu.data;
  const botaoSair = (
    <button
      type="button"
      className="min-h-11 rounded-xl px-4 font-bold underline"
      onClick={() => sair.mutate(undefined, { onSuccess: () => void navegar("/admin/entrar") })}
    >
      Sair
    </button>
  );

  return (
    <ContextoDaConta value={conta}>
      <div className="mx-auto flex min-h-dvh max-w-6xl flex-col gap-6 px-4 py-4">
        <header className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-col">
            <span className="text-2xl font-extrabold">UniQuiz · Admin</span>
            <span className="text-sm">
              {conta.nome} · {PERFIS_DO_PAINEL[conta.perfil].rotulo}
            </span>
          </div>
          {conta.precisaTrocarSenha ? (
            botaoSair
          ) : (
            <nav aria-label="Menu do admin">
              <ul className="flex flex-wrap gap-2">
                {secoesPermitidas(conta).map((secao) => (
                  <li key={secao.para}>
                    <NavLink
                      to={secao.para}
                      end={secao.para === "/admin"}
                      className={({ isActive }) =>
                        `flex min-h-11 items-center rounded-xl px-4 font-extrabold ${isActive ? "bg-primaria text-white" : "bg-neutro"}`
                      }
                    >
                      {secao.rotulo}
                    </NavLink>
                  </li>
                ))}
                <li>{botaoSair}</li>
              </ul>
            </nav>
          )}
        </header>
        <main>{conta.precisaTrocarSenha ? <TrocaDeSenhaObrigatoria conta={conta} /> : <Outlet />}</main>
      </div>
    </ContextoDaConta>
  );
}

function TrocaDeSenhaObrigatoria({ conta }: { conta: ContaLogada }) {
  return (
    <section aria-labelledby="titulo-troca-obrigatoria" className="flex flex-col gap-4">
      <h1 id="titulo-troca-obrigatoria" className="text-3xl font-extrabold">
        Crie a sua senha
      </h1>
      <p className="max-w-prose">
        Olá, {conta.nome}. Você entrou com uma senha provisória, criada por outra pessoa. Antes de usar o
        painel, escolha uma senha que só você conheça.
      </p>
      <FormularioDeTrocaDeSenha rotuloDaSenhaAtual="Senha provisória" />
    </section>
  );
}

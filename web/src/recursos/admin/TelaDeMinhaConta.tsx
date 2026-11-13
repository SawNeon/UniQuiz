import { useEncerrarMinhaSessao, useEncerrarMinhasOutrasSessoes, useMinhasSessoes } from "../../api/admin";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { FormularioDeTrocaDeSenha } from "./FormularioDeTrocaDeSenha";
import { formatarMomento, PERFIS_DO_PAINEL, useContaDoPainel } from "./painel";

export function TelaDeMinhaConta() {
  const conta = useContaDoPainel();
  const perfil = PERFIS_DO_PAINEL[conta.perfil];

  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-3xl font-extrabold">Minha conta</h1>
      <dl className="grid max-w-md grid-cols-[auto_1fr] gap-x-4 gap-y-2 rounded-padrao bg-neutro/60 p-4">
        <dt className="font-extrabold">Nome</dt>
        <dd>{conta.nome}</dd>
        <dt className="font-extrabold">Login</dt>
        <dd>{conta.usuario}</dd>
        <dt className="font-extrabold">Perfil</dt>
        <dd>
          {perfil.rotulo}
          <span className="block text-sm">{perfil.descricao}</span>
        </dd>
      </dl>
      <MinhasSessoes />
      <section aria-labelledby="titulo-troca" className="flex flex-col gap-3">
        <h2 id="titulo-troca" className="text-2xl font-extrabold">
          Trocar senha
        </h2>
        <FormularioDeTrocaDeSenha />
      </section>
    </div>
  );
}

function MinhasSessoes() {
  const sessoes = useMinhasSessoes();
  const encerrar = useEncerrarMinhaSessao();
  const encerrarOutras = useEncerrarMinhasOutrasSessoes();
  const temOutras = sessoes.data?.some((sessao) => !sessao.atual) ?? false;

  return (
    <section aria-labelledby="titulo-sessoes" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="titulo-sessoes" className="text-2xl font-extrabold">
          Onde você está conectado
        </h2>
        {temOutras && (
          <Botao
            variante="secundario"
            disabled={encerrarOutras.isPending}
            onClick={() => encerrarOutras.mutate()}
          >
            Sair dos outros aparelhos
          </Botao>
        )}
      </div>
      <p className="max-w-prose text-sm">
        Por segurança, o painel desconecta depois de 30 minutos sem uso e, no máximo, 8 horas depois de
        entrar. Se não reconhecer algum aparelho, encerre a sessão e troque a sua senha.
      </p>
      {sessoes.isPending && <Carregando />}
      {sessoes.isError && <MensagemDeErro erro={sessoes.error} />}
      {(encerrar.error ?? encerrarOutras.error) && (
        <MensagemDeErro erro={encerrar.error ?? encerrarOutras.error} />
      )}
      <ul className="flex flex-col gap-2">
        {sessoes.data?.map((sessao) => (
          <li
            key={sessao.id}
            aria-label={sessao.dispositivo ?? "Aparelho desconhecido"}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-neutro/60 p-3"
          >
            <div className="flex flex-col">
              <span className="font-extrabold">
                {sessao.dispositivo ?? "Aparelho desconhecido"}
                {sessao.atual && (
                  <span className="ml-2 rounded-lg bg-acerto-suave px-2 py-0.5 text-sm">Este aparelho</span>
                )}
              </span>
              <span className="text-sm">
                {sessao.ip ? `IP ${sessao.ip} · ` : ""}Entrou em {formatarMomento(sessao.criadaEm)} · Último
                uso em {formatarMomento(sessao.ultimoUsoEm)}
              </span>
            </div>
            {!sessao.atual && (
              <Botao
                variante="secundario"
                disabled={encerrar.isPending}
                onClick={() => encerrar.mutate(sessao.id)}
              >
                Encerrar
              </Botao>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

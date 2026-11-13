import { FormularioDeTrocaDeSenha } from "./FormularioDeTrocaDeSenha";
import { PERFIS_DO_PAINEL, useContaDoPainel } from "./painel";

export function TelaDeMinhaConta() {
  const conta = useContaDoPainel();
  const perfil = PERFIS_DO_PAINEL[conta.perfil];

  return (
    <div className="flex flex-col gap-6">
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
      <section aria-labelledby="titulo-troca" className="flex flex-col gap-3">
        <h2 id="titulo-troca" className="text-2xl font-extrabold">
          Trocar senha
        </h2>
        <FormularioDeTrocaDeSenha />
      </section>
    </div>
  );
}

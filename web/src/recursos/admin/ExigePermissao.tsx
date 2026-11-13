import type { ReactNode } from "react";
import { Link } from "react-router";
import type { Permissao } from "../../api/tipos";
import { pode, useContaDoPainel } from "./painel";

export function ExigePermissao({ permissao, children }: { permissao: Permissao; children: ReactNode }) {
  const conta = useContaDoPainel();
  if (pode(conta, permissao)) {
    return children;
  }
  return (
    <section className="flex flex-col items-start gap-3 rounded-padrao bg-neutro/60 p-6">
      <h1 className="text-2xl font-extrabold">Sem acesso a esta área</h1>
      <p>O seu perfil não permite abrir esta parte do painel. Se precisar, fale com um Administrador.</p>
      <Link to="/admin" className="min-h-11 font-extrabold underline">
        Voltar ao início do painel
      </Link>
    </section>
  );
}

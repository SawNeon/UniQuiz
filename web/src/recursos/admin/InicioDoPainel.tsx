import { Navigate } from "react-router";
import { pode, secoesPermitidas, useContaDoPainel } from "./painel";
import { TelaDeConteudo } from "./TelaDeConteudo";

export function InicioDoPainel() {
  const conta = useContaDoPainel();
  if (pode(conta, "GERIR_CONTEUDO")) {
    return <TelaDeConteudo />;
  }
  const primeira = secoesPermitidas(conta)[0];
  return <Navigate to={primeira?.para ?? "/admin/minha-conta"} replace />;
}

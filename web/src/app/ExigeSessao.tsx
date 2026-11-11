import { Navigate, Outlet } from "react-router";
import { naoAutenticado } from "../api/cliente";
import { usePerfil } from "../api/consultas";
import { Carregando, MensagemDeErro } from "../componentes/ui/Estados";

export function ExigeSessao() {
  const perfil = usePerfil();

  if (perfil.isPending) {
    return <Carregando />;
  }
  if (naoAutenticado(perfil.error)) {
    return <Navigate to="/entrar" replace />;
  }
  if (perfil.isError) {
    return <MensagemDeErro erro={perfil.error} aoTentarDeNovo={() => void perfil.refetch()} />;
  }
  return <Outlet />;
}

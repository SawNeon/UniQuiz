import { createBrowserRouter } from "react-router";
import { LayoutAdmin } from "../recursos/admin/LayoutAdmin";
import { LoginAdmin } from "../recursos/admin/LoginAdmin";
import { TelaDaLicaoAdmin } from "../recursos/admin/TelaDaLicaoAdmin";
import { TelaDeColaboradores } from "../recursos/admin/TelaDeColaboradores";
import { TelaDeConteudo } from "../recursos/admin/TelaDeConteudo";
import { TelaDeTemporadas } from "../recursos/admin/TelaDeTemporadas";
import { TelaDeEntrada } from "../recursos/acesso/TelaDeEntrada";
import { TelaDoCaminho } from "../recursos/caminho/TelaDoCaminho";
import { TelaDeValidacao } from "../recursos/certificado/TelaDeValidacao";
import { TelaDeClassificacao } from "../recursos/classificacao/TelaDeClassificacao";
import { TelaDeEstudo } from "../recursos/estudo/TelaDeEstudo";
import { TelaDaLicao } from "../recursos/licao/TelaDaLicao";
import { TelaDePerfil } from "../recursos/perfil/TelaDePerfil";
import { ExigeSessao } from "./ExigeSessao";
import { LayoutDoColaborador } from "./LayoutDoColaborador";

export const rotas = createBrowserRouter([
  { path: "/entrar", element: <TelaDeEntrada /> },
  { path: "/certificado/:codigo", element: <TelaDeValidacao /> },
  {
    element: <ExigeSessao />,
    children: [
      {
        element: <LayoutDoColaborador />,
        children: [
          { path: "/", element: <TelaDoCaminho /> },
          { path: "/classificacao", element: <TelaDeClassificacao /> },
          { path: "/perfil", element: <TelaDePerfil /> },
        ],
      },
      { path: "/modulos/:moduloId/estudo", element: <TelaDeEstudo /> },
      { path: "/licoes/:licaoId", element: <TelaDaLicao /> },
    ],
  },
  { path: "/admin/entrar", element: <LoginAdmin /> },
  {
    path: "/admin",
    element: <LayoutAdmin />,
    children: [
      { index: true, element: <TelaDeConteudo /> },
      { path: "licoes/:licaoId", element: <TelaDaLicaoAdmin /> },
      { path: "colaboradores", element: <TelaDeColaboradores /> },
      { path: "temporadas", element: <TelaDeTemporadas /> },
    ],
  },
]);

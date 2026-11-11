import { createBrowserRouter } from "react-router";
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
]);

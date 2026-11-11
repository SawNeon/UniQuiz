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
  {
    path: "/admin/entrar",
    lazy: () => import("../recursos/admin/LoginAdmin").then((modulo) => ({ Component: modulo.LoginAdmin })),
  },
  {
    path: "/admin",
    lazy: () => import("../recursos/admin/LayoutAdmin").then((modulo) => ({ Component: modulo.LayoutAdmin })),
    children: [
      {
        index: true,
        lazy: () =>
          import("../recursos/admin/TelaDeConteudo").then((modulo) => ({ Component: modulo.TelaDeConteudo })),
      },
      {
        path: "licoes/:licaoId",
        lazy: () =>
          import("../recursos/admin/TelaDaLicaoAdmin").then((modulo) => ({
            Component: modulo.TelaDaLicaoAdmin,
          })),
      },
      {
        path: "colaboradores",
        lazy: () =>
          import("../recursos/admin/TelaDeColaboradores").then((modulo) => ({
            Component: modulo.TelaDeColaboradores,
          })),
      },
      {
        path: "temporadas",
        lazy: () =>
          import("../recursos/admin/TelaDeTemporadas").then((modulo) => ({
            Component: modulo.TelaDeTemporadas,
          })),
      },
    ],
  },
]);

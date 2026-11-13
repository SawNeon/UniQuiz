import type { ComponentType } from "react";
import { createBrowserRouter, Navigate, type RouteObject } from "react-router";
import type { Permissao } from "../api/tipos";
import { TelaDeEntrada } from "../recursos/acesso/TelaDeEntrada";
import { TelaDoCaminho } from "../recursos/caminho/TelaDoCaminho";
import { TelaDeValidacao } from "../recursos/certificado/TelaDeValidacao";
import { TelaDeClassificacao } from "../recursos/classificacao/TelaDeClassificacao";
import { TelaDeEstudo } from "../recursos/estudo/TelaDeEstudo";
import { TelaDaLicao } from "../recursos/licao/TelaDaLicao";
import { TelaDePerfil } from "../recursos/perfil/TelaDePerfil";
import { ExigeSessao } from "./ExigeSessao";
import { LayoutDoColaborador } from "./LayoutDoColaborador";
import { ErroDaRota, TelaDePaginaNaoEncontrada } from "./TelasDeErro";

function telaProtegida(
  caminho: string,
  permissao: Permissao,
  carregar: () => Promise<ComponentType>,
): RouteObject {
  return {
    path: caminho,
    lazy: async () => {
      const [{ ExigePermissao }, Tela] = await Promise.all([
        import("../recursos/admin/ExigePermissao"),
        carregar(),
      ]);
      return {
        Component: () => (
          <ExigePermissao permissao={permissao}>
            <Tela />
          </ExigePermissao>
        ),
      };
    },
  };
}

export const rotas = createBrowserRouter([
  {
    errorElement: <ErroDaRota />,
    children: [
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
        lazy: () =>
          import("../recursos/admin/LoginAdmin").then((modulo) => ({ Component: modulo.LoginAdmin })),
      },
      {
        path: "/admin",
        lazy: () =>
          import("../recursos/admin/LayoutAdmin").then((modulo) => ({ Component: modulo.LayoutAdmin })),
        children: [
          {
            index: true,
            lazy: () =>
              import("../recursos/admin/InicioDoPainel").then((modulo) => ({
                Component: modulo.InicioDoPainel,
              })),
          },
          telaProtegida("licoes/:licaoId", "GERIR_CONTEUDO", () =>
            import("../recursos/admin/TelaDaLicaoAdmin").then((modulo) => modulo.TelaDaLicaoAdmin),
          ),
          telaProtegida("colaboradores", "GERIR_COLABORADORES", () =>
            import("../recursos/admin/TelaDeColaboradores").then((modulo) => modulo.TelaDeColaboradores),
          ),
          telaProtegida("temporadas", "GERIR_TEMPORADAS", () =>
            import("../recursos/admin/TelaDeTemporadas").then((modulo) => modulo.TelaDeTemporadas),
          ),
          telaProtegida("usuarios", "GERIR_USUARIOS_DO_PAINEL", () =>
            import("../recursos/admin/TelaDeUsuarios").then((modulo) => modulo.TelaDeUsuarios),
          ),
          {
            path: "minha-conta",
            lazy: () =>
              import("../recursos/admin/TelaDeMinhaConta").then((modulo) => ({
                Component: modulo.TelaDeMinhaConta,
              })),
          },
        ],
      },
      { path: "/index.html", element: <Navigate to="/" replace /> },
      { path: "*", element: <TelaDePaginaNaoEncontrada /> },
    ],
  },
]);

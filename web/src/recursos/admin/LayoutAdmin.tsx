import { Navigate, NavLink, Outlet, useNavigate } from "react-router";
import { useAdminEu, useSairAdmin } from "../../api/admin";
import { naoAutenticado } from "../../api/cliente";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";

const itens = [
  { para: "/admin", rotulo: "Conteúdo" },
  { para: "/admin/colaboradores", rotulo: "Colaboradores" },
  { para: "/admin/temporadas", rotulo: "Temporadas" },
];

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

  return (
    <div className="mx-auto flex min-h-dvh max-w-6xl flex-col gap-6 px-4 py-4">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <span className="text-2xl font-extrabold">UniQuiz · Admin</span>
        <nav aria-label="Menu do admin">
          <ul className="flex flex-wrap gap-2">
            {itens.map((item) => (
              <li key={item.para}>
                <NavLink
                  to={item.para}
                  end
                  className={({ isActive }) =>
                    `flex min-h-11 items-center rounded-xl px-4 font-extrabold ${isActive ? "bg-primaria text-white" : "bg-neutro"}`
                  }
                >
                  {item.rotulo}
                </NavLink>
              </li>
            ))}
            <li>
              <button
                type="button"
                className="min-h-11 rounded-xl px-4 font-bold underline"
                onClick={() => sair.mutate(undefined, { onSuccess: () => void navegar("/admin/entrar") })}
              >
                Sair
              </button>
            </li>
          </ul>
        </nav>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}

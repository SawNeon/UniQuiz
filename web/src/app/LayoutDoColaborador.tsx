import { NavLink, Outlet } from "react-router";
import { usePerfil } from "../api/consultas";
import { Icone, type NomeDoIcone } from "../componentes/ui/Icone";
import { alternarSom, tocar, useSomLigado } from "../sons/sons";

const itens: { para: string; rotulo: string; icone: NomeDoIcone }[] = [
  { para: "/", rotulo: "Caminho", icone: "caminho" },
  { para: "/classificacao", rotulo: "Classificação", icone: "trofeu" },
  { para: "/perfil", rotulo: "Perfil", icone: "perfil" },
];

export function LayoutDoColaborador() {
  const perfil = usePerfil();
  const somLigado = useSomLigado();

  function aoAlternarSom() {
    alternarSom();
    tocar("clique");
  }

  return (
    <div className="mx-auto flex min-h-dvh max-w-xl flex-col">
      <header className="flex items-center justify-between gap-3 px-4 py-3">
        <div
          className="flex items-center gap-1.5"
          aria-label={`Sequência de ${perfil.data?.diasSeguidos ?? 0} dias`}
        >
          <Icone nome="chama" className="fill-energia text-energia" />
          <span className="font-extrabold">{perfil.data?.diasSeguidos ?? 0} dias</span>
        </div>
        <span className="font-numeros text-2xl font-bold">
          {(perfil.data?.pontosNaTemporada ?? 0).toLocaleString("pt-BR")} pontos
        </span>
        <button
          type="button"
          onClick={aoAlternarSom}
          aria-label={somLigado ? "Desligar o som" : "Ligar o som"}
          aria-pressed={somLigado}
          className="flex size-11 items-center justify-center rounded-padrao bg-neutro transition-transform active:scale-95 motion-reduce:transition-none"
        >
          <Icone nome={somLigado ? "somLigado" : "somDesligado"} />
        </button>
      </header>

      <main className="flex-1 px-4 pb-24">
        <Outlet />
      </main>

      <nav
        aria-label="Menu principal"
        className="fixed inset-x-0 bottom-0 border-t border-neutro bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur"
      >
        <ul className="mx-auto grid max-w-xl grid-cols-3">
          {itens.map((item) => (
            <li key={item.para}>
              <NavLink
                to={item.para}
                end
                onClick={() => tocar("navegar")}
                className={({ isActive }) =>
                  [
                    "relative flex min-h-14 flex-col items-center justify-center gap-0.5 text-[11px] transition-colors motion-reduce:transition-none",
                    isActive
                      ? "font-extrabold text-primaria"
                      : "font-semibold text-texto/55 hover:text-texto",
                  ].join(" ")
                }
              >
                {({ isActive }) => (
                  <>
                    <span
                      aria-hidden="true"
                      className={[
                        "absolute top-0 h-0.5 w-8 rounded-full bg-primaria transition-opacity motion-reduce:transition-none",
                        isActive ? "opacity-100" : "opacity-0",
                      ].join(" ")}
                    />
                    <Icone nome={item.icone} strokeWidth={isActive ? 2.25 : 1.75} />
                    {item.rotulo}
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}

import { NavLink, Outlet } from "react-router";
import { usePerfil } from "../api/consultas";
import { Icone, type NomeDoIcone } from "../componentes/ui/Icone";
import { alternarSom, useSomLigado } from "../sons/sons";

const itens: { para: string; rotulo: string; icone: NomeDoIcone }[] = [
  { para: "/", rotulo: "Caminho", icone: "caminho" },
  { para: "/classificacao", rotulo: "Classificação", icone: "trofeu" },
  { para: "/perfil", rotulo: "Perfil", icone: "perfil" },
];

export function LayoutDoColaborador() {
  const perfil = usePerfil();
  const somLigado = useSomLigado();

  return (
    <div className="mx-auto flex min-h-dvh max-w-xl flex-col">
      <header className="flex items-center justify-between gap-3 px-4 py-3">
        <div className="flex items-center gap-1.5" aria-label={`Sequência de ${perfil.data?.diasSeguidos ?? 0} dias`}>
          <Icone nome="chama" className="fill-energia text-energia" />
          <span className="font-extrabold">{perfil.data?.diasSeguidos ?? 0} dias</span>
        </div>
        <span className="font-numeros text-2xl font-bold">
          {(perfil.data?.pontosNaTemporada ?? 0).toLocaleString("pt-BR")} pontos
        </span>
        <button
          type="button"
          onClick={alternarSom}
          aria-label={somLigado ? "Desligar o som" : "Ligar o som"}
          aria-pressed={somLigado}
          className="flex size-11 items-center justify-center rounded-padrao bg-neutro"
        >
          <Icone nome={somLigado ? "somLigado" : "somDesligado"} />
        </button>
      </header>

      <main className="flex-1 px-4 pb-24">
        <Outlet />
      </main>

      <nav
        aria-label="Menu principal"
        className="fixed inset-x-0 bottom-0 border-t-2 border-neutro bg-white pb-[env(safe-area-inset-bottom)]"
      >
        <ul className="mx-auto grid max-w-xl grid-cols-3">
          {itens.map((item) => (
            <li key={item.para}>
              <NavLink
                to={item.para}
                end
                className={({ isActive }) =>
                  [
                    "flex min-h-16 flex-col items-center justify-center gap-1 border-t-4 text-sm",
                    isActive ? "border-primaria font-extrabold" : "border-transparent font-semibold",
                  ].join(" ")
                }
              >
                <Icone nome={item.icone} />
                {item.rotulo}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}

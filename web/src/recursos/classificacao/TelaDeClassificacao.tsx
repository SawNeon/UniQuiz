import { useClassificacao } from "../../api/consultas";
import type { Posicao } from "../../api/tipos";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Icone } from "../../componentes/ui/Icone";

const MEDALHAS: Record<number, string> = { 2: "Prata", 3: "Bronze" };

export function TelaDeClassificacao() {
  const classificacao = useClassificacao();

  if (classificacao.isPending) {
    return <Carregando />;
  }
  if (classificacao.isError) {
    return <MensagemDeErro erro={classificacao.error} aoTentarDeNovo={() => void classificacao.refetch()} />;
  }

  const { temporada, topo, minhaPosicao, pontosParaSubir } = classificacao.data;
  if (!temporada) {
    return (
      <p className="py-10 text-center text-lg font-bold">
        Nenhuma temporada aberta agora. A próxima começa em breve!
      </p>
    );
  }

  const foraDoTopo = minhaPosicao && !topo.some((posicao) => posicao.souEu);

  return (
    <div className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-3xl font-extrabold">Classificação</h1>
        <p className="font-bold">{temporada.nome}</p>
        {temporada.premioTexto && (
          <p className="mt-2 rounded-padrao bg-aviso p-3 font-bold">
            Prêmio do 1º lugar: {temporada.premioTexto}
          </p>
        )}
      </header>

      {topo.length === 0 ? (
        <p className="text-lg">Ninguém pontuou ainda nesta temporada. Que tal ser a primeira pessoa?</p>
      ) : (
        <ol className="flex flex-col gap-2.5">
          {topo.map((posicao) => (
            <LinhaDaClassificacao key={posicao.posicao} posicao={posicao} />
          ))}
        </ol>
      )}

      {foraDoTopo && minhaPosicao && (
        <section className="flex flex-col gap-2 rounded-padrao bg-acerto-suave p-4">
          <p className="text-lg font-extrabold">Você está em {minhaPosicao.posicao}º lugar</p>
          {pontosParaSubir && <p>Faltam {pontosParaSubir} pontos para subir uma posição. Bora?</p>}
        </section>
      )}
      {!minhaPosicao && (
        <p className="rounded-padrao bg-neutro p-4 font-bold">Faça uma lição para entrar na classificação.</p>
      )}
    </div>
  );
}

function LinhaDaClassificacao({ posicao }: { posicao: Posicao }) {
  const lider = posicao.posicao === 1;
  return (
    <li
      className={[
        "flex items-center gap-3 rounded-padrao p-3",
        lider ? "border-4 border-conquista bg-white" : "bg-neutro",
        posicao.souEu ? "outline-3 outline-primaria" : "",
      ].join(" ")}
    >
      <span className="w-10 text-center font-numeros text-2xl font-bold">{posicao.posicao}º</span>
      <div className="flex flex-1 flex-col">
        <span className="flex items-center gap-1.5 font-extrabold">
          {lider && <Icone nome="coroa" className="fill-conquista" />}
          {posicao.nome}
          {posicao.souEu && <span className="text-sm font-bold">(você)</span>}
        </span>
        <span className="text-sm">
          {posicao.setor}
          {lider && " · Referência em Excelência"}
          {MEDALHAS[posicao.posicao] && ` · Medalha de ${MEDALHAS[posicao.posicao]}`}
        </span>
      </div>
      <span className="font-numeros text-2xl font-bold">{posicao.valor.toLocaleString("pt-BR")}</span>
    </li>
  );
}

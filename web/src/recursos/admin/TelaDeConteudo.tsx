import { useState } from "react";
import { Link } from "react-router";
import {
  chavesAdmin,
  useCriarLicao,
  useModulosAdmin,
  useOrdem,
  useRenomearLicao,
  useStatus,
} from "../../api/admin";
import type { ModuloParaAdmin, PorNivel } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { BotoesDeOrdem, Entrada, SeletorDeStatus } from "./componentes";
import { FormularioDeModulo } from "./FormularioDeModulo";
import { totalDeDesafios } from "./niveis";
import { EditorDeQuantidades, SorteioDaLicao } from "./SorteioDaLicao";

export function TelaDeConteudo() {
  const modulos = useModulosAdmin();
  const [criando, setCriando] = useState(false);
  const ordem = useOrdem("/admin/modulos/ordem", [chavesAdmin.modulos]);

  if (modulos.isPending) {
    return <Carregando />;
  }
  if (modulos.isError) {
    return <MensagemDeErro erro={modulos.error} aoTentarDeNovo={() => void modulos.refetch()} />;
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-extrabold">Caminho</h1>
        {!criando && <Botao onClick={() => setCriando(true)}>Novo módulo</Botao>}
      </div>
      <p>
        O colaborador vê só o que está publicado. Publique os desafios, depois a lição e por último o módulo.
        Em cada lição você escolhe quantos desafios de cada nível a pessoa responde; o banco pode ter mais, e
        o sistema sorteia a cada tentativa.
      </p>
      {criando && <FormularioDeModulo aoTerminar={() => setCriando(false)} />}
      {ordem.isError && <MensagemDeErro erro={ordem.error} />}
      {modulos.data.map((modulo, indice) => (
        <CartaoDoModulo
          key={modulo.id}
          modulo={modulo}
          indice={indice}
          todos={modulos.data}
          aoReordenar={(ids) => ordem.mutate(ids)}
        />
      ))}
    </div>
  );
}

function CartaoDoModulo({
  modulo,
  indice,
  todos,
  aoReordenar,
}: {
  modulo: ModuloParaAdmin;
  indice: number;
  todos: ModuloParaAdmin[];
  aoReordenar: (ids: number[]) => void;
}) {
  const [editando, setEditando] = useState(false);
  const [novaLicao, setNovaLicao] = useState("");
  const [novasQuantidades, setNovasQuantidades] = useState<PorNivel>({ facil: 2, medio: 2, dificil: 1 });
  const statusDoModulo = useStatus("modulos");
  const statusDaLicao = useStatus("licoes");
  const criarLicao = useCriarLicao();
  const ordemDasLicoes = useOrdem(`/admin/modulos/${modulo.id}/licoes/ordem`, [chavesAdmin.modulos]);
  const erro = statusDoModulo.error ?? statusDaLicao.error ?? criarLicao.error ?? ordemDasLicoes.error;

  return (
    <section
      aria-labelledby={`modulo-${modulo.id}`}
      className="flex flex-col gap-4 rounded-padrao bg-neutro/60 p-4"
    >
      <div className="flex flex-wrap items-center gap-3">
        <BotoesDeOrdem
          itens={todos}
          indice={indice}
          aoReordenar={aoReordenar}
          nome={`o módulo ${modulo.titulo}`}
        />
        <h2 id={`modulo-${modulo.id}`} className="flex-1 text-xl font-extrabold">
          {modulo.titulo}
        </h2>
        <SeletorDeStatus
          rotulo={`Situação do módulo ${modulo.titulo}`}
          status={modulo.status}
          aoMudar={(status) => statusDoModulo.mutate({ id: modulo.id, status })}
        />
        <Botao variante="discreto" onClick={() => setEditando((atual) => !atual)}>
          {editando ? "Fechar" : "Editar e material"}
        </Botao>
      </div>
      {erro && <MensagemDeErro erro={erro} />}
      {editando && <FormularioDeModulo modulo={modulo} aoTerminar={() => setEditando(false)} />}

      <ol className="flex flex-col gap-2">
        {modulo.licoes.map((licao, posicao) => (
          <li key={licao.id} className="flex flex-wrap items-center gap-3 rounded-xl bg-white p-3">
            <BotoesDeOrdem
              itens={modulo.licoes}
              indice={posicao}
              aoReordenar={(ids) => ordemDasLicoes.mutate(ids)}
              nome={`a lição ${licao.titulo}`}
            />
            <TituloEditavel licaoId={licao.id} titulo={licao.titulo} />
            <span className="text-sm font-bold">{licao.desafiosPublicados} desafios publicados</span>
            <SeletorDeStatus
              rotulo={`Situação da lição ${licao.titulo}`}
              status={licao.status}
              aoMudar={(status) => statusDaLicao.mutate({ id: licao.id, status })}
            />
            <Link
              to={`/admin/licoes/${licao.id}`}
              className="min-h-11 content-center rounded-xl bg-primaria px-4 font-extrabold text-white"
            >
              Desafios
            </Link>
            <SorteioDaLicao key={JSON.stringify(licao.quantidades)} licao={licao} />
          </li>
        ))}
      </ol>

      <form
        className="flex flex-col gap-3 rounded-xl bg-white p-3"
        onSubmit={(evento) => {
          evento.preventDefault();
          if (novaLicao.trim()) {
            criarLicao.mutate(
              { moduloId: modulo.id, titulo: novaLicao, quantidades: novasQuantidades },
              { onSuccess: () => setNovaLicao("") },
            );
          }
        }}
      >
        <div className="flex flex-wrap gap-2">
          <Entrada
            aria-label={`Título da nova lição em ${modulo.titulo}`}
            placeholder="Título da nova lição"
            value={novaLicao}
            onChange={(evento) => setNovaLicao(evento.target.value)}
            className="flex-1"
          />
          <Botao type="submit" disabled={criarLicao.isPending}>
            Adicionar lição
          </Botao>
        </div>
        <p className="text-sm">
          Quantos desafios de cada nível cada pessoa responde. O banco da lição pode ter mais desafios do que
          isso: a cada tentativa o sistema sorteia, em ordem aleatória. Esta lição terá{" "}
          <strong>{totalDeDesafios(novasQuantidades)}</strong> desafios por tentativa.
        </p>
        <EditorDeQuantidades
          valor={novasQuantidades}
          aoMudar={setNovasQuantidades}
          rotuloDaLicao="na nova lição"
        />
      </form>
    </section>
  );
}

function TituloEditavel({ licaoId, titulo }: { licaoId: number; titulo: string }) {
  const [valor, setValor] = useState(titulo);
  const renomear = useRenomearLicao();
  return (
    <Entrada
      aria-label="Título da lição"
      value={valor}
      onChange={(evento) => setValor(evento.target.value)}
      onBlur={() => valor.trim() && valor !== titulo && renomear.mutate({ licaoId, titulo: valor })}
      className="min-w-48 flex-1 font-bold"
    />
  );
}

import { useState } from "react";
import { Link, useParams } from "react-router";
import {
  chavesAdmin,
  useDesafiosAdmin,
  useDuplicarDesafio,
  useModulosAdmin,
  useOrdem,
  useStatus,
} from "../../api/admin";
import type { DesafioParaAdmin } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Icone } from "../../componentes/ui/Icone";
import { BotoesDeOrdem, SeletorDeStatus } from "./componentes";
import { EditorDeDesafio } from "./EditorDeDesafio";

type Edicao = { modo: "novo" } | { modo: "editar"; desafio: DesafioParaAdmin } | null;

const TIPOS = {
  MULTIPLA_ESCOLHA: "Múltipla escolha",
  VERDADEIRO_OU_FALSO: "Verdadeiro ou falso",
  COMPLETAR_CLICANDO: "Completar clicando",
};

export function TelaDaLicaoAdmin() {
  const licaoId = Number(useParams().licaoId);
  const desafios = useDesafiosAdmin(licaoId);
  const modulos = useModulosAdmin();
  const [edicao, setEdicao] = useState<Edicao>(null);
  const status = useStatus("desafios", licaoId);
  const duplicar = useDuplicarDesafio(licaoId);
  const ordem = useOrdem(`/admin/licoes/${licaoId}/desafios/ordem`, [chavesAdmin.desafios(licaoId)]);

  const licao = modulos.data
    ?.flatMap((modulo) => modulo.licoes)
    .find((candidata) => candidata.id === licaoId);
  const erro = status.error ?? duplicar.error ?? ordem.error;

  return (
    <div className="flex flex-col gap-6">
      <Link to="/admin" className="flex min-h-11 items-center gap-2 self-start font-extrabold">
        <Icone nome="voltar" />
        Voltar ao caminho
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-extrabold">{licao?.titulo ?? "Lição"}</h1>
        {!edicao && <Botao onClick={() => setEdicao({ modo: "novo" })}>Novo desafio</Botao>}
      </div>
      <p>Uma lição publicada precisa ter de 5 a 8 desafios publicados.</p>
      {erro && <MensagemDeErro erro={erro} />}

      {edicao && (
        <EditorDeDesafio
          key={edicao.modo === "editar" ? edicao.desafio.id : "novo"}
          licaoId={licaoId}
          desafio={edicao.modo === "editar" ? edicao.desafio : undefined}
          aoTerminar={() => setEdicao(null)}
        />
      )}

      {desafios.isPending && <Carregando />}
      {desafios.isError && <MensagemDeErro erro={desafios.error} />}
      {desafios.isSuccess && (
        <ol className="flex flex-col gap-2">
          {desafios.data.map((desafio, indice) => (
            <li key={desafio.id} className="flex flex-wrap items-center gap-3 rounded-xl bg-neutro/60 p-3">
              <BotoesDeOrdem
                itens={desafios.data}
                indice={indice}
                aoReordenar={(ids) => ordem.mutate(ids)}
                nome={`o desafio ${indice + 1}`}
              />
              <div className="flex min-w-48 flex-1 flex-col">
                <span className="font-extrabold">{desafio.enunciado}</span>
                <span className="text-sm">
                  {TIPOS[desafio.tipo]} · {desafio.pontos} pontos · versão {desafio.versao}
                </span>
              </div>
              <SeletorDeStatus
                rotulo={`Situação do desafio ${indice + 1}`}
                status={desafio.status}
                aoMudar={(novo) => status.mutate({ id: desafio.id, status: novo })}
              />
              <Botao variante="secundario" onClick={() => setEdicao({ modo: "editar", desafio })}>
                Editar
              </Botao>
              <Botao variante="discreto" onClick={() => duplicar.mutate(desafio.id)}>
                Duplicar
              </Botao>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

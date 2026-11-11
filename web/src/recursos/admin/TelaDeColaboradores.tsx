import { useState } from "react";
import { useAjustarColaborador, useColaboradoresAdmin, useImportacao, useZerarPontos } from "../../api/admin";
import type { ColaboradorParaAdmin, ResumoDaImportacao } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";

export function TelaDeColaboradores() {
  return (
    <div className="flex flex-col gap-10">
      <Importacao />
      <Busca />
    </div>
  );
}

function Importacao() {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const previa = useImportacao("previa");
  const confirmar = useImportacao("confirmar");
  const resumo: ResumoDaImportacao | undefined = confirmar.data ?? previa.data;

  return (
    <section aria-labelledby="titulo-importacao" className="flex flex-col gap-4">
      <h1 id="titulo-importacao" className="text-3xl font-extrabold">
        Importar a planilha do RH
      </h1>
      <p>
        Envie um arquivo .xlsx ou .csv com as colunas <strong>nome</strong>, <strong>setor</strong> e{" "}
        <strong>empresa</strong>. Nada é gravado até você confirmar. Quem sair da planilha fica inativo, sem
        perder os pontos.
      </p>
      <Campo rotulo="Planilha">
        <input
          type="file"
          accept=".xlsx,.csv"
          onChange={(evento) => {
            setArquivo(evento.target.files?.[0] ?? null);
            previa.reset();
            confirmar.reset();
          }}
          className="min-h-11"
        />
      </Campo>
      <div className="flex flex-wrap gap-3">
        <Botao disabled={!arquivo || previa.isPending} onClick={() => arquivo && previa.mutate(arquivo)}>
          Ver prévia
        </Botao>
        {previa.data?.podeConfirmar && !confirmar.isSuccess && (
          <Botao disabled={confirmar.isPending} onClick={() => arquivo && confirmar.mutate(arquivo)}>
            Confirmar importação
          </Botao>
        )}
      </div>
      {(previa.error ?? confirmar.error) && <MensagemDeErro erro={previa.error ?? confirmar.error} />}

      {resumo && (
        <div className="flex flex-col gap-3 rounded-padrao bg-neutro/60 p-4" role="status">
          <p className="font-extrabold">
            {confirmar.isSuccess ? "Importação concluída." : "Prévia da importação"}
          </p>
          <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            <li>Novos: {resumo.novos}</li>
            <li>Mantidos: {resumo.mantidos}</li>
            <li>Reativados: {resumo.reativados}</li>
            <li>Inativados: {resumo.inativados}</li>
          </ul>
          {[...resumo.problemas, ...resumo.homonimos].map((aviso) => (
            <p key={aviso} className="rounded-xl bg-aviso p-2 text-sm font-bold">
              {aviso}
            </p>
          ))}
        </div>
      )}
    </section>
  );
}

function Busca() {
  const [busca, setBusca] = useState("");
  const colaboradores = useColaboradoresAdmin(busca);

  return (
    <section aria-labelledby="titulo-busca" className="flex flex-col gap-4">
      <h2 id="titulo-busca" className="text-2xl font-extrabold">
        Ajustar colaboradores
      </h2>
      <Entrada
        type="search"
        aria-label="Buscar pelo nome"
        placeholder="Buscar pelo nome"
        value={busca}
        onChange={(evento) => setBusca(evento.target.value)}
      />
      {colaboradores.isError && <MensagemDeErro erro={colaboradores.error} />}
      <ul className="flex flex-col gap-2">
        {colaboradores.data?.map((colaborador) => (
          <LinhaDoColaborador key={colaborador.id} colaborador={colaborador} />
        ))}
      </ul>
    </section>
  );
}

function LinhaDoColaborador({ colaborador }: { colaborador: ColaboradorParaAdmin }) {
  const [nome, setNome] = useState(colaborador.nome);
  const ajustar = useAjustarColaborador();
  const zerar = useZerarPontos();

  return (
    <li className="flex flex-wrap items-center gap-3 rounded-xl bg-neutro/60 p-3">
      <Entrada
        aria-label={`Nome de ${colaborador.nome}`}
        value={nome}
        onChange={(evento) => setNome(evento.target.value)}
        className="min-w-56 flex-1"
      />
      <span className="text-sm">
        {colaborador.setor} · {colaborador.empresa}
      </span>
      <label className="flex min-h-11 items-center gap-2 font-bold">
        <input
          type="checkbox"
          checked={colaborador.ativo}
          onChange={(evento) => ajustar.mutate({ id: colaborador.id, nome, ativo: evento.target.checked })}
          className="size-5 accent-[#00995d]"
        />
        Ativo
      </label>
      <Botao
        variante="secundario"
        disabled={nome.trim() === "" || nome === colaborador.nome}
        onClick={() => ajustar.mutate({ id: colaborador.id, nome, ativo: colaborador.ativo })}
      >
        Salvar nome
      </Botao>
      <Botao
        variante="discreto"
        onClick={() => {
          if (window.confirm(`Zerar os pontos de ${colaborador.nome} na temporada atual?`)) {
            zerar.mutate(colaborador.id);
          }
        }}
      >
        Zerar pontos
      </Botao>
      {(ajustar.error ?? zerar.error) && <MensagemDeErro erro={ajustar.error ?? zerar.error} />}
      {zerar.isSuccess && <span className="text-sm font-bold">Pontos zerados.</span>}
    </li>
  );
}

import { type FormEvent, useState } from "react";
import {
  enderecoDoZip,
  useCertificadosEmitidos,
  useLiberarCertificado,
  useModulosComCertificado,
} from "../../api/certificados";
import { useFiltrosDosRelatorios } from "../../api/relatorios";
import type { FiltroDeCertificados, Permissao } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada, SeloDeStatus } from "./componentes";
import { EditorDoCertificado } from "./EditorDoCertificado";
import { pode, useContaDoPainel } from "./painel";

type Aba = "emitidos" | "liberacao" | "modelo";

const ABAS: { id: Aba; rotulo: string; permissao: Permissao }[] = [
  { id: "emitidos", rotulo: "Emitidos", permissao: "VER_RELATORIOS" },
  { id: "liberacao", rotulo: "Liberação", permissao: "GERIR_CERTIFICADOS" },
  { id: "modelo", rotulo: "Modelo", permissao: "GERIR_CERTIFICADOS" },
];

const ESTILO_DA_SELECAO = "min-h-11 rounded-xl border-2 border-neutro bg-white px-2";

const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });

function dataCurta(dia: string): string {
  return DATA.format(new Date(`${dia}T12:00:00`));
}

function numeroOuNada(valor: string): number | undefined {
  return valor ? Number(valor) : undefined;
}

export function TelaDeCertificados() {
  const conta = useContaDoPainel();
  const abas = ABAS.filter((item) => pode(conta, item.permissao));
  const [aba, setAba] = useState<Aba>(abas[0]?.id ?? "emitidos");

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold">Certificados</h1>
        <p className="max-w-prose">
          Quem concluiu um módulo já tem o certificado garantido. Ele só fica disponível para a pessoa baixar
          depois que o módulo for liberado.
        </p>
      </div>
      {abas.length > 1 && (
        <div role="tablist" aria-label="Certificados" className="flex flex-wrap gap-2">
          {abas.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              id={`aba-${item.id}`}
              aria-selected={aba === item.id}
              aria-controls={`painel-${item.id}`}
              onClick={() => setAba(item.id)}
              className={`min-h-11 rounded-xl px-4 font-extrabold ${aba === item.id ? "bg-primaria text-white" : "bg-neutro"}`}
            >
              {item.rotulo}
            </button>
          ))}
        </div>
      )}
      <div role="tabpanel" id={`painel-${aba}`} aria-labelledby={`aba-${aba}`}>
        {aba === "emitidos" && <AbaDeEmitidos />}
        {aba === "liberacao" && <AbaDeLiberacao />}
        {aba === "modelo" && <EditorDoCertificado />}
      </div>
    </div>
  );
}

function AbaDeEmitidos() {
  const conta = useContaDoPainel();
  const filtros = useFiltrosDosRelatorios();
  const [filtro, setFiltro] = useState<FiltroDeCertificados>({ pagina: 0 });
  const [busca, setBusca] = useState("");
  const emitidos = useCertificadosEmitidos(filtro);
  const totalDePaginas = emitidos.data
    ? Math.max(1, Math.ceil(emitidos.data.total / emitidos.data.tamanho))
    : 1;
  const filtroDoZip = { moduloId: filtro.moduloId, setorId: filtro.setorId, busca: filtro.busca };

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    setFiltro((anterior) => ({ ...anterior, busca: busca.trim() || undefined, pagina: 0 }));
  }

  return (
    <div className="flex flex-col gap-6">
      <form onSubmit={buscar} className="flex flex-wrap items-end gap-3">
        <Campo rotulo="Módulo">
          <select
            value={filtro.moduloId ?? ""}
            onChange={(evento) =>
              setFiltro((anterior) => ({
                ...anterior,
                moduloId: numeroOuNada(evento.target.value),
                pagina: 0,
              }))
            }
            className={ESTILO_DA_SELECAO}
          >
            <option value="">Todos os módulos</option>
            {filtros.data?.modulos.map((modulo) => (
              <option key={modulo.id} value={modulo.id}>
                {modulo.titulo}
              </option>
            ))}
          </select>
        </Campo>
        <Campo rotulo="Setor">
          <select
            value={filtro.setorId ?? ""}
            onChange={(evento) =>
              setFiltro((anterior) => ({
                ...anterior,
                setorId: numeroOuNada(evento.target.value),
                pagina: 0,
              }))
            }
            className={ESTILO_DA_SELECAO}
          >
            <option value="">Todos os setores que você acompanha</option>
            {filtros.data?.setores.map((setor) => (
              <option key={setor.id} value={setor.id}>
                {setor.empresa} · {setor.nome}
              </option>
            ))}
          </select>
        </Campo>
        <Campo rotulo="Nome ou código">
          <Entrada value={busca} maxLength={100} onChange={(evento) => setBusca(evento.target.value)} />
        </Campo>
        <Botao type="submit" variante="secundario">
          Buscar
        </Botao>
      </form>

      {emitidos.isPending && <Carregando />}
      {emitidos.isError && <MensagemDeErro erro={emitidos.error} />}
      {emitidos.data && (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-lg font-extrabold">
              {emitidos.data.total === 1
                ? "1 certificado emitido"
                : `${emitidos.data.total} certificados emitidos`}
            </p>
            {pode(conta, "EXPORTAR_RELATORIOS") && emitidos.data.total > 0 && (
              <a
                href={enderecoDoZip(filtroDoZip)}
                download
                className="flex min-h-11 items-center rounded-xl bg-primaria px-4 font-extrabold text-white"
              >
                Baixar todos em ZIP ({emitidos.data.total})
              </a>
            )}
          </div>
          {pode(conta, "EXPORTAR_RELATORIOS") && emitidos.data.total > 0 && (
            <p className="max-w-prose text-sm">
              O ZIP traz um PDF por pessoa, separado por módulo, e uma lista em CSV. Com muitas pessoas, o
              download pode levar alguns minutos.
            </p>
          )}
          {emitidos.data.itens.length === 0 ? (
            <p>Nenhum certificado emitido com esses filtros.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[40rem] text-left">
                <thead>
                  <tr className="border-b-2 border-neutro">
                    <th className="py-2 pr-3">Nome</th>
                    <th className="py-2 pr-3">Setor</th>
                    <th className="py-2 pr-3">Módulo</th>
                    <th className="py-2 pr-3">Emitido em</th>
                    <th className="py-2">Código</th>
                  </tr>
                </thead>
                <tbody>
                  {emitidos.data.itens.map((certificado) => (
                    <tr key={certificado.codigo} className="border-b border-neutro">
                      <td className="py-2 pr-3 font-bold">{certificado.nome}</td>
                      <td className="py-2 pr-3">
                        {certificado.empresa} · {certificado.setor}
                      </td>
                      <td className="py-2 pr-3">{certificado.modulo}</td>
                      <td className="py-2 pr-3">{dataCurta(certificado.emitidoEm)}</td>
                      <td className="py-2 font-numeros">{certificado.codigoFormatado}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {totalDePaginas > 1 && (
            <div className="flex items-center gap-3">
              <Botao
                variante="secundario"
                disabled={filtro.pagina === 0}
                onClick={() => setFiltro((anterior) => ({ ...anterior, pagina: anterior.pagina - 1 }))}
              >
                Anterior
              </Botao>
              <span className="font-bold">
                Página {filtro.pagina + 1} de {totalDePaginas}
              </span>
              <Botao
                variante="secundario"
                disabled={filtro.pagina + 1 >= totalDePaginas}
                onClick={() => setFiltro((anterior) => ({ ...anterior, pagina: anterior.pagina + 1 }))}
              >
                Próxima
              </Botao>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function AbaDeLiberacao() {
  const modulos = useModulosComCertificado();
  const liberar = useLiberarCertificado();

  if (modulos.isPending) {
    return <Carregando />;
  }
  if (modulos.isError) {
    return <MensagemDeErro erro={modulos.error} />;
  }
  return (
    <div className="flex flex-col gap-4">
      <p className="max-w-prose">
        Módulo novo começa travado. Enquanto estiver travado, quem conclui vê que o certificado está
        garantido, mas ainda não consegue baixar nem validar. Ao liberar, todos que já concluíram passam a
        baixar na hora.
      </p>
      {modulos.data.length === 0 ? (
        <p>Nenhum módulo criado ainda.</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {modulos.data.map((modulo) => (
            <li
              key={modulo.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-padrao bg-neutro/50 p-4"
            >
              <div className="flex flex-col gap-1">
                <p className="flex flex-wrap items-center gap-2 text-lg font-extrabold">
                  {modulo.titulo} <SeloDeStatus status={modulo.status} />
                </p>
                <p>
                  {modulo.emitidos === 1
                    ? "1 certificado garantido"
                    : `${modulo.emitidos} certificados garantidos`}
                  {" · "}
                  <strong>{modulo.liberado ? "Liberado para baixar" : "Travado"}</strong>
                </p>
              </div>
              <Botao
                variante={modulo.liberado ? "secundario" : "principal"}
                disabled={liberar.isPending}
                onClick={() => liberar.mutate({ moduloId: modulo.id, liberado: !modulo.liberado })}
              >
                {modulo.liberado ? `Travar ${modulo.titulo}` : `Liberar ${modulo.titulo}`}
              </Botao>
            </li>
          ))}
        </ul>
      )}
      {liberar.isError && <MensagemDeErro erro={liberar.error} />}
    </div>
  );
}

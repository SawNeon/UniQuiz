import { type FormEvent, type ReactNode, useState } from "react";
import {
  useAcaoSobreUsuario,
  useAcoesAuditadas,
  useAlertasDeAuditoria,
  useRegistroDeAuditoria,
} from "../../api/admin";
import type {
  AlertasDeAuditoria,
  FiltroDeAuditoria,
  RegistroDeAuditoria,
  ResultadoDaAcao,
  TipoDeAtor,
} from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";
import { formatarMomento, pode, useContaDoPainel } from "./painel";

const TIPOS_DE_ATOR: Record<TipoDeAtor, string> = {
  ADMINISTRADOR: "Usuário do painel",
  COLABORADOR: "Colaborador",
  ANONIMO: "Não identificado",
};

const SEM_FILTRO: FiltroDeAuditoria = { pagina: 0 };

const ESTILO_DA_SELECAO = "min-h-11 rounded-xl border-2 border-neutro bg-white px-2";

export function TelaDeAuditoria() {
  const [filtro, setFiltro] = useState<FiltroDeAuditoria>(SEM_FILTRO);
  const registro = useRegistroDeAuditoria(filtro);

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold">Auditoria</h1>
        <p className="max-w-prose">
          Tudo o que muda dados no painel, cada entrada e saída e cada tentativa que falhou fica registrado
          aqui. O registro não pode ser alterado nem apagado.
        </p>
      </div>
      <Alertas />
      <section aria-labelledby="titulo-registro" className="flex flex-col gap-4">
        <h2 id="titulo-registro" className="text-2xl font-extrabold">
          Registro
        </h2>
        <FiltrosDaAuditoria
          key={JSON.stringify(filtro)}
          inicial={filtro}
          aoFiltrar={(novo) => setFiltro({ ...novo, pagina: 0 })}
          aoLimpar={() => setFiltro(SEM_FILTRO)}
        />
        {registro.isPending && <Carregando />}
        {registro.isError && <MensagemDeErro erro={registro.error} />}
        {registro.data && (
          <>
            <p className="font-bold" aria-live="polite">
              {registro.data.total === 1 ? "1 registro" : `${registro.data.total} registros`}
            </p>
            <TabelaDoRegistro itens={registro.data.itens} />
            <Paginacao
              pagina={registro.data.pagina}
              totalDePaginas={Math.max(1, Math.ceil(registro.data.total / registro.data.tamanho))}
              aoMudar={(pagina) => setFiltro({ ...filtro, pagina })}
            />
          </>
        )}
      </section>
    </div>
  );
}

function Alertas() {
  const alertas = useAlertasDeAuditoria();

  return (
    <section
      aria-labelledby="titulo-alertas"
      className="flex flex-col gap-3 rounded-padrao border-2 border-neutro p-4"
    >
      <h2 id="titulo-alertas" className="text-2xl font-extrabold">
        Alertas das últimas 24 horas
      </h2>
      {alertas.isPending && <Carregando />}
      {alertas.isError && <MensagemDeErro erro={alertas.error} />}
      {alertas.data && <ConteudoDosAlertas alertas={alertas.data} />}
    </section>
  );
}

function ConteudoDosAlertas({ alertas }: { alertas: AlertasDeAuditoria }) {
  const conta = useContaDoPainel();
  const desbloquear = useAcaoSobreUsuario();
  const podeDesbloquear = pode(conta, "GERIR_USUARIOS_DO_PAINEL");
  const vazio =
    alertas.falhasPorPessoa.length === 0 &&
    alertas.falhasPorIp.length === 0 &&
    alertas.contasBloqueadas.length === 0;

  if (vazio) {
    return <p>Nenhum alerta. Sem bloqueios nem tentativas de acesso repetidas.</p>;
  }

  return (
    <div className="grid gap-4 md:grid-cols-3">
      <ListaDeAlertas titulo="Contas bloqueadas agora" vazio="Nenhuma conta bloqueada.">
        {alertas.contasBloqueadas.map((conta) => (
          <li key={`${conta.tipo}-${conta.id}`} className="flex flex-col gap-1">
            <span className="font-bold">
              {conta.nome}{" "}
              <span className="font-normal">({conta.tipo === "ADMINISTRADOR" ? "painel" : "app"})</span>
            </span>
            <span className="text-sm">Libera sozinha em {formatarMomento(conta.ate)}</span>
            {conta.tipo === "ADMINISTRADOR" && podeDesbloquear && (
              <Botao
                variante="secundario"
                className="self-start"
                disabled={desbloquear.isPending}
                onClick={() => desbloquear.mutate({ id: conta.id, acao: "desbloquear" })}
              >
                Desbloquear {conta.nome}
              </Botao>
            )}
          </li>
        ))}
      </ListaDeAlertas>
      <ListaDeAlertas
        titulo="Muitas falhas de entrada por pessoa"
        vazio="Nenhuma pessoa com 3 ou mais falhas."
      >
        {alertas.falhasPorPessoa.map((falha, indice) => (
          <li key={`${falha.quem ?? "?"}-${indice}`}>
            <span className="font-bold">{falha.quem ?? "Não identificado"}</span>
            <span className="block text-sm">
              {falha.falhas} falhas · última em {formatarMomento(falha.ultima)}
            </span>
          </li>
        ))}
      </ListaDeAlertas>
      <ListaDeAlertas titulo="Muitas falhas vindas do mesmo IP" vazio="Nenhum IP com 5 ou mais falhas.">
        {alertas.falhasPorIp.map((falha) => (
          <li key={falha.ip}>
            <span className="font-bold">{falha.ip}</span>
            <span className="block text-sm">
              {falha.falhas} falhas · última em {formatarMomento(falha.ultima)}
            </span>
          </li>
        ))}
      </ListaDeAlertas>
      {desbloquear.isError && (
        <div className="md:col-span-3">
          <MensagemDeErro erro={desbloquear.error} />
        </div>
      )}
    </div>
  );
}

function ListaDeAlertas({
  titulo,
  vazio,
  children,
}: {
  titulo: string;
  vazio: string;
  children: ReactNode[];
}) {
  return (
    <div className="flex flex-col gap-2 rounded-xl bg-neutro/50 p-3">
      <h3 className="font-extrabold">{titulo}</h3>
      {children.length === 0 ? (
        <p className="text-sm">{vazio}</p>
      ) : (
        <ul className="flex flex-col gap-3">{children}</ul>
      )}
    </div>
  );
}

function FiltrosDaAuditoria({
  inicial,
  aoFiltrar,
  aoLimpar,
}: {
  inicial: FiltroDeAuditoria;
  aoFiltrar: (filtro: FiltroDeAuditoria) => void;
  aoLimpar: () => void;
}) {
  const acoes = useAcoesAuditadas();
  const [rascunho, setRascunho] = useState<FiltroDeAuditoria>(inicial);

  function mudar<K extends keyof FiltroDeAuditoria>(campo: K, valor: FiltroDeAuditoria[K]) {
    setRascunho((atual) => ({ ...atual, [campo]: valor || undefined }));
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    aoFiltrar({ ...rascunho, busca: rascunho.busca?.trim() || undefined });
  }

  return (
    <form
      aria-label="Filtros da auditoria"
      onSubmit={enviar}
      className="grid gap-3 rounded-padrao bg-neutro/40 p-4 sm:grid-cols-2 lg:grid-cols-3"
    >
      <Campo rotulo="Quem">
        <select
          className={ESTILO_DA_SELECAO}
          value={rascunho.atorTipo ?? ""}
          onChange={(evento) => mudar("atorTipo", evento.target.value as TipoDeAtor)}
        >
          <option value="">Todos</option>
          {Object.entries(TIPOS_DE_ATOR).map(([valor, rotulo]) => (
            <option key={valor} value={valor}>
              {rotulo}
            </option>
          ))}
        </select>
      </Campo>
      <Campo rotulo="Ação">
        <select
          className={ESTILO_DA_SELECAO}
          value={rascunho.acao ?? ""}
          onChange={(evento) => mudar("acao", evento.target.value)}
        >
          <option value="">Todas</option>
          {acoes.data?.map((acao) => (
            <option key={acao.codigo} value={acao.codigo}>
              {acao.descricao}
            </option>
          ))}
        </select>
      </Campo>
      <Campo rotulo="Resultado">
        <select
          className={ESTILO_DA_SELECAO}
          value={rascunho.resultado ?? ""}
          onChange={(evento) => mudar("resultado", evento.target.value as ResultadoDaAcao)}
        >
          <option value="">Todos</option>
          <option value="SUCESSO">Deu certo</option>
          <option value="FALHA">Falhou</option>
        </select>
      </Campo>
      <Campo rotulo="Nome, alvo ou IP">
        <Entrada
          type="search"
          maxLength={100}
          value={rascunho.busca ?? ""}
          onChange={(evento) => mudar("busca", evento.target.value)}
        />
      </Campo>
      <Campo rotulo="De">
        <Entrada
          type="date"
          value={rascunho.de ?? ""}
          onChange={(evento) => mudar("de", evento.target.value)}
        />
      </Campo>
      <Campo rotulo="Até">
        <Entrada
          type="date"
          value={rascunho.ate ?? ""}
          onChange={(evento) => mudar("ate", evento.target.value)}
        />
      </Campo>
      <div className="flex flex-wrap gap-3 sm:col-span-2 lg:col-span-3">
        <Botao type="submit">Filtrar</Botao>
        <Botao variante="secundario" onClick={aoLimpar}>
          Limpar filtros
        </Botao>
      </div>
    </form>
  );
}

function TabelaDoRegistro({ itens }: { itens: RegistroDeAuditoria[] }) {
  if (itens.length === 0) {
    return <p>Nenhum registro com esses filtros.</p>;
  }
  return (
    <div className="overflow-x-auto rounded-padrao border-2 border-neutro">
      <table className="w-full min-w-[44rem] border-collapse text-left text-sm">
        <thead className="bg-neutro">
          <tr>
            <th scope="col" className="p-2">
              Quando
            </th>
            <th scope="col" className="p-2">
              Quem
            </th>
            <th scope="col" className="p-2">
              O quê
            </th>
            <th scope="col" className="p-2">
              Resultado
            </th>
            <th scope="col" className="p-2">
              IP
            </th>
          </tr>
        </thead>
        <tbody>
          {itens.map((item) => (
            <tr key={item.id} className="border-t-2 border-neutro align-top">
              <td className="p-2 whitespace-nowrap tabular-nums">{formatarMomento(item.momento)}</td>
              <td className="p-2">
                <span className="font-bold">{item.atorNome ?? "Não identificado"}</span>
                <span className="block">{TIPOS_DE_ATOR[item.atorTipo]}</span>
              </td>
              <td className="p-2">
                {item.acaoDescricao}
                {item.alvo && <span className="block text-xs">{item.alvo}</span>}
              </td>
              <td className="p-2">
                <span
                  className={`rounded-lg px-2 py-0.5 font-bold ${item.resultado === "SUCESSO" ? "bg-acerto-suave" : "bg-erro-suave"}`}
                >
                  {item.resultado === "SUCESSO" ? "Deu certo" : "Falhou"}
                </span>
                {item.detalhe && <span className="mt-1 block">{item.detalhe}</span>}
              </td>
              <td className="p-2 tabular-nums">{item.ip ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Paginacao({
  pagina,
  totalDePaginas,
  aoMudar,
}: {
  pagina: number;
  totalDePaginas: number;
  aoMudar: (pagina: number) => void;
}) {
  if (totalDePaginas <= 1) {
    return null;
  }
  return (
    <nav aria-label="Páginas do registro" className="flex flex-wrap items-center gap-3">
      <Botao variante="secundario" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>
        Anterior
      </Botao>
      <span className="font-bold">
        Página {pagina + 1} de {totalDePaginas}
      </span>
      <Botao
        variante="secundario"
        disabled={pagina + 1 >= totalDePaginas}
        onClick={() => aoMudar(pagina + 1)}
      >
        Próxima
      </Botao>
    </nav>
  );
}

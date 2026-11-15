import { type FormEvent, type ReactNode, useState } from "react";
import {
  enderecoDaExportacao,
  useConformidade,
  useDesempenho,
  useEngajamento,
  useFiltrosDosRelatorios,
  usePessoasDaConformidade,
} from "../../api/relatorios";
import type {
  AvaliacaoDaCalibracao,
  Dificuldade,
  FiltroDeDesempenho,
  FiltroDePessoas,
  FiltrosDosRelatorios,
  RelatorioExportavel,
  SituacaoNoModulo,
} from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";
import { BarraDePercentual, BarraEmpilhada, GraficoDeColunas } from "./graficos";
import { formatarMomento, formatarPercentual, pode, useContaDoPainel } from "./painel";

type Aba = "conformidade" | "desempenho" | "engajamento";

const ABAS: { id: Aba; rotulo: string }[] = [
  { id: "conformidade", rotulo: "Conformidade" },
  { id: "desempenho", rotulo: "Desempenho" },
  { id: "engajamento", rotulo: "Engajamento" },
];

const SITUACOES: Record<SituacaoNoModulo, string> = {
  CONCLUIU: "Concluiu",
  EM_ANDAMENTO: "Em andamento",
  NAO_COMECOU: "Não começou",
};

const DIFICULDADES: Record<Dificuldade, string> = { FACIL: "Fácil", MEDIO: "Médio", DIFICIL: "Difícil" };

const CALIBRACOES: Record<AvaliacaoDaCalibracao, { rotulo: string; cor: string }> = {
  ADEQUADA: { rotulo: "Adequada", cor: "bg-acerto-suave" },
  MAIS_FACIL_QUE_O_PREVISTO: { rotulo: "Mais fácil que o previsto", cor: "bg-info" },
  MAIS_DIFICIL_QUE_O_PREVISTO: { rotulo: "Mais difícil que o previsto", cor: "bg-erro-suave" },
  POUCOS_DADOS: { rotulo: "Poucos dados", cor: "bg-neutro" },
};

const SEMANAS = [4, 12, 26, 52];

const ESTILO_DA_SELECAO = "min-h-11 rounded-xl border-2 border-neutro bg-white px-2";

const FATIAS = {
  concluiram: { rotulo: "Concluíram", cor: "bg-primaria" },
  emAndamento: { rotulo: "Em andamento", cor: "bg-conquista" },
  naoComecaram: { rotulo: "Não começaram", cor: "bg-texto/30" },
};

const DATA_CURTA = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

function dataCurta(dia: string): string {
  return DATA_CURTA.format(new Date(`${dia}T12:00:00`));
}

function numeroOuNada(valor: string): number | undefined {
  return valor ? Number(valor) : undefined;
}

export function TelaDeRelatorios() {
  const [aba, setAba] = useState<Aba>("conformidade");
  const filtros = useFiltrosDosRelatorios();
  const conta = useContaDoPainel();

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold">Relatórios</h1>
        <p className="max-w-prose">
          Quem já fez os módulos, como as pessoas estão indo nos desafios e quantas estudaram em cada semana.
          {conta.perfil === "GESTOR" && filtros.data
            ? ` Você vê apenas: ${filtros.data.setores.map((setor) => setor.nome).join(", ") || "nenhum setor"}.`
            : ""}
        </p>
      </div>
      <div role="tablist" aria-label="Relatórios" className="flex flex-wrap gap-2">
        {ABAS.map((item) => (
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
      {filtros.isPending && <Carregando />}
      {filtros.isError && <MensagemDeErro erro={filtros.error} />}
      {filtros.data && (
        <div role="tabpanel" id={`painel-${aba}`} aria-labelledby={`aba-${aba}`}>
          {aba === "conformidade" && <AbaDeConformidade filtros={filtros.data} />}
          {aba === "desempenho" && <AbaDeDesempenho filtros={filtros.data} />}
          {aba === "engajamento" && <AbaDeEngajamento />}
        </div>
      )}
    </div>
  );
}

function BotoesDeExportacao({
  relatorio,
  valores,
  rotulo,
}: {
  relatorio: RelatorioExportavel;
  valores: Record<string, string | number | undefined>;
  rotulo: string;
}) {
  const conta = useContaDoPainel();
  if (!pode(conta, "EXPORTAR_RELATORIOS")) {
    return null;
  }
  return (
    <div className="flex flex-wrap gap-2">
      <a
        href={enderecoDaExportacao(relatorio, "CSV", valores)}
        download
        className="flex min-h-11 items-center rounded-xl bg-neutro px-4 font-bold"
      >
        Baixar {rotulo} em CSV
      </a>
      <a
        href={enderecoDaExportacao(relatorio, "XLSX", valores)}
        download
        className="flex min-h-11 items-center rounded-xl bg-neutro px-4 font-bold"
      >
        Baixar {rotulo} em Excel
      </a>
    </div>
  );
}

function Secao({ titulo, children, acoes }: { titulo: string; children: ReactNode; acoes?: ReactNode }) {
  const id = `titulo-${titulo.toLowerCase().replace(/\s+/g, "-")}`;
  return (
    <section aria-labelledby={id} className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id={id} className="text-2xl font-extrabold">
          {titulo}
        </h2>
        {acoes}
      </div>
      {children}
    </section>
  );
}

function Numero({ rotulo, valor }: { rotulo: string; valor: number | string }) {
  return (
    <div className="flex flex-col rounded-padrao bg-neutro/60 p-4">
      <span className="text-sm font-bold">{rotulo}</span>
      <span className="font-numeros text-4xl font-bold">{valor}</span>
    </div>
  );
}

function AbaDeConformidade({ filtros }: { filtros: FiltrosDosRelatorios }) {
  const [setorId, setSetorId] = useState<number | undefined>(undefined);
  const conformidade = useConformidade(setorId);

  return (
    <div className="flex flex-col gap-8">
      <Campo rotulo="Setor">
        <select
          value={setorId ?? ""}
          onChange={(evento) => setSetorId(numeroOuNada(evento.target.value))}
          className={`${ESTILO_DA_SELECAO} max-w-md`}
        >
          <option value="">Todos os setores que você acompanha</option>
          {filtros.setores.map((setor) => (
            <option key={setor.id} value={setor.id}>
              {setor.empresa} · {setor.nome}
            </option>
          ))}
        </select>
      </Campo>
      {conformidade.isPending && <Carregando />}
      {conformidade.isError && <MensagemDeErro erro={conformidade.error} />}
      {conformidade.data && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Numero rotulo="Pessoas ativas" valor={conformidade.data.pessoas} />
            <Numero rotulo="Concluíram a trilha" valor={conformidade.data.trilha.concluiram} />
            <Numero rotulo="Em andamento" valor={conformidade.data.trilha.emAndamento} />
            <Numero rotulo="Não começaram" valor={conformidade.data.trilha.naoComecaram} />
          </div>
          <Secao titulo="Por módulo">
            {conformidade.data.modulos.length === 0 ? (
              <p>Nenhum módulo publicado ainda.</p>
            ) : (
              <ul className="flex flex-col gap-4">
                {conformidade.data.modulos.map((modulo) => (
                  <li key={modulo.id} className="flex flex-col gap-1">
                    <span className="font-extrabold">
                      {modulo.titulo}{" "}
                      <span className="text-sm font-normal">
                        ({modulo.licoes} {modulo.licoes === 1 ? "lição" : "lições"})
                      </span>
                    </span>
                    <BarraEmpilhada
                      rotulo={`Módulo ${modulo.titulo}`}
                      fatias={[
                        { ...FATIAS.concluiram, valor: modulo.concluiram },
                        { ...FATIAS.emAndamento, valor: modulo.emAndamento },
                        { ...FATIAS.naoComecaram, valor: modulo.naoComecaram },
                      ]}
                    />
                  </li>
                ))}
              </ul>
            )}
          </Secao>
          <Secao titulo="Por setor">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[40rem] text-left">
                <thead>
                  <tr className="border-b-2 border-neutro">
                    <th className="p-2">Setor</th>
                    <th className="p-2">Pessoas</th>
                    <th className="p-2">Concluíram a trilha</th>
                    <th className="p-2">Lições feitas, em média</th>
                  </tr>
                </thead>
                <tbody>
                  {conformidade.data.setores.map((setor) => (
                    <tr key={setor.id} className="border-b border-neutro">
                      <td className="p-2">
                        <span className="font-bold">{setor.setor}</span>
                        <span className="block text-sm">{setor.empresa}</span>
                      </td>
                      <td className="p-2">{setor.pessoas}</td>
                      <td className="p-2">{setor.concluiramATrilha}</td>
                      <td className="p-2">
                        <BarraDePercentual
                          valor={setor.licoesConcluidas}
                          rotulo={`Lições feitas em ${setor.setor}`}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Secao>
        </>
      )}
      <ListaDePessoas filtros={filtros} setorId={setorId} />
    </div>
  );
}

function ListaDePessoas({ filtros, setorId }: { filtros: FiltrosDosRelatorios; setorId?: number }) {
  const [filtro, setFiltro] = useState<Omit<FiltroDePessoas, "setorId">>({ pagina: 0 });
  const [busca, setBusca] = useState("");
  const completo: FiltroDePessoas = { ...filtro, setorId };
  const pessoas = usePessoasDaConformidade(completo);
  const paraExportar = { setorId, moduloId: filtro.moduloId, situacao: filtro.situacao, busca: filtro.busca };

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    setFiltro({ ...filtro, busca: busca.trim() || undefined, pagina: 0 });
  }

  return (
    <Secao
      titulo="Pessoas"
      acoes={<BotoesDeExportacao relatorio="conformidade-pessoas" rotulo="a lista" valores={paraExportar} />}
    >
      <form onSubmit={buscar} className="grid gap-3 md:grid-cols-4">
        <Campo rotulo="Módulo">
          <select
            value={filtro.moduloId ?? ""}
            onChange={(evento) =>
              setFiltro({ ...filtro, moduloId: numeroOuNada(evento.target.value), pagina: 0 })
            }
            className={ESTILO_DA_SELECAO}
          >
            <option value="">A trilha inteira</option>
            {filtros.modulos.map((modulo) => (
              <option key={modulo.id} value={modulo.id}>
                {modulo.titulo}
              </option>
            ))}
          </select>
        </Campo>
        <Campo rotulo="Situação">
          <select
            value={filtro.situacao ?? ""}
            onChange={(evento) =>
              setFiltro({
                ...filtro,
                situacao: (evento.target.value || undefined) as SituacaoNoModulo | undefined,
                pagina: 0,
              })
            }
            className={ESTILO_DA_SELECAO}
          >
            <option value="">Todas</option>
            {Object.entries(SITUACOES).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>
                {rotulo}
              </option>
            ))}
          </select>
        </Campo>
        <Campo rotulo="Nome">
          <Entrada type="search" value={busca} onChange={(evento) => setBusca(evento.target.value)} />
        </Campo>
        <Botao type="submit" className="self-end">
          Buscar
        </Botao>
      </form>
      {pessoas.isPending && <Carregando />}
      {pessoas.isError && <MensagemDeErro erro={pessoas.error} />}
      {pessoas.data && (
        <>
          <p className="font-bold" aria-live="polite">
            {pessoas.data.total === 1 ? "1 pessoa" : `${pessoas.data.total} pessoas`}
          </p>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[44rem] text-left">
              <thead>
                <tr className="border-b-2 border-neutro">
                  <th className="p-2">Nome</th>
                  <th className="p-2">Setor</th>
                  <th className="p-2">Lições</th>
                  <th className="p-2">Situação</th>
                  <th className="p-2">Última atividade</th>
                </tr>
              </thead>
              <tbody>
                {pessoas.data.itens.map((pessoa) => (
                  <tr key={pessoa.id} className="border-b border-neutro">
                    <td className="p-2 font-bold">{pessoa.nome}</td>
                    <td className="p-2">
                      {pessoa.setor}
                      <span className="block text-sm">{pessoa.empresa}</span>
                    </td>
                    <td className="p-2">
                      {pessoa.licoesConcluidas} de {pessoa.totalDeLicoes}
                    </td>
                    <td className="p-2">{SITUACOES[pessoa.situacao]}</td>
                    <td className="p-2">
                      {pessoa.ultimaAtividade ? formatarMomento(pessoa.ultimaAtividade) : "Nunca"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginas
            pagina={pessoas.data.pagina}
            total={Math.max(1, Math.ceil(pessoas.data.total / pessoas.data.tamanho))}
            aoMudar={(pagina) => setFiltro({ ...filtro, pagina })}
          />
        </>
      )}
    </Secao>
  );
}

function Paginas({
  pagina,
  total,
  aoMudar,
}: {
  pagina: number;
  total: number;
  aoMudar: (pagina: number) => void;
}) {
  if (total <= 1) {
    return null;
  }
  return (
    <nav aria-label="Páginas da lista" className="flex items-center gap-3">
      <Botao variante="secundario" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>
        Anterior
      </Botao>
      <span>
        Página {pagina + 1} de {total}
      </span>
      <Botao variante="secundario" disabled={pagina >= total - 1} onClick={() => aoMudar(pagina + 1)}>
        Próxima
      </Botao>
    </nav>
  );
}

function AbaDeDesempenho({ filtros }: { filtros: FiltrosDosRelatorios }) {
  const [filtro, setFiltro] = useState<FiltroDeDesempenho>({});
  const [rascunho, setRascunho] = useState<FiltroDeDesempenho>({});
  const desempenho = useDesempenho(filtro);
  const valores = { de: filtro.de, ate: filtro.ate, moduloId: filtro.moduloId };

  return (
    <div className="flex flex-col gap-8">
      <form
        className="grid gap-3 md:grid-cols-4"
        onSubmit={(evento) => {
          evento.preventDefault();
          setFiltro(rascunho);
        }}
      >
        <Campo rotulo="De">
          <Entrada
            type="date"
            value={rascunho.de ?? ""}
            onChange={(evento) => setRascunho({ ...rascunho, de: evento.target.value || undefined })}
          />
        </Campo>
        <Campo rotulo="Até">
          <Entrada
            type="date"
            value={rascunho.ate ?? ""}
            onChange={(evento) => setRascunho({ ...rascunho, ate: evento.target.value || undefined })}
          />
        </Campo>
        <Campo rotulo="Módulo do desempenho">
          <select
            value={rascunho.moduloId ?? ""}
            onChange={(evento) => setRascunho({ ...rascunho, moduloId: numeroOuNada(evento.target.value) })}
            className={ESTILO_DA_SELECAO}
          >
            <option value="">Todos</option>
            {filtros.modulos.map((modulo) => (
              <option key={modulo.id} value={modulo.id}>
                {modulo.titulo}
              </option>
            ))}
          </select>
        </Campo>
        <Botao type="submit" className="self-end">
          Aplicar
        </Botao>
      </form>
      {desempenho.isPending && <Carregando />}
      {desempenho.isError && <MensagemDeErro erro={desempenho.error} />}
      {desempenho.data && (
        <>
          <p className="font-bold">
            De {dataCurta(desempenho.data.de)} a {dataCurta(desempenho.data.ate)}
          </p>
          <Secao
            titulo="Por lição"
            acoes={<BotoesDeExportacao relatorio="desempenho-licoes" rotulo="as lições" valores={valores} />}
          >
            {desempenho.data.licoes.length === 0 ? (
              <p>Ninguém fez lições neste período.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[48rem] text-left">
                  <thead>
                    <tr className="border-b-2 border-neutro">
                      <th className="p-2">Lição</th>
                      <th className="p-2">Iniciadas</th>
                      <th className="p-2">Concluídas</th>
                      <th className="p-2">Perfeitas</th>
                      <th className="p-2">Abandono</th>
                      <th className="p-2">Acerto na 1ª resposta</th>
                    </tr>
                  </thead>
                  <tbody>
                    {desempenho.data.licoes.map((licao) => (
                      <tr key={licao.id} className="border-b border-neutro">
                        <td className="p-2">
                          <span className="font-bold">{licao.licao}</span>
                          <span className="block text-sm">{licao.modulo}</span>
                        </td>
                        <td className="p-2">{licao.iniciadas}</td>
                        <td className="p-2">{licao.concluidas}</td>
                        <td className="p-2">{licao.perfeitas}</td>
                        <td className="p-2">{formatarPercentual(licao.abandono)}</td>
                        <td className="p-2">
                          <BarraDePercentual
                            valor={licao.acertoNaPrimeira}
                            rotulo={`Acerto na primeira resposta em ${licao.licao}`}
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Secao>
          <Secao
            titulo="Por desafio"
            acoes={
              <BotoesDeExportacao relatorio="desempenho-desafios" rotulo="os desafios" valores={valores} />
            }
          >
            <p className="max-w-prose text-sm">
              Os desafios com menos acerto aparecem primeiro. A calibração compara o acerto real com a
              dificuldade escolhida no cadastro, a partir de 10 respostas.
            </p>
            {desempenho.data.desafios.length === 0 ? (
              <p>Nenhuma resposta neste período.</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {desempenho.data.desafios.map((desafio) => (
                  <li
                    key={desafio.id}
                    className="flex flex-col gap-2 rounded-padrao border-2 border-neutro p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <p className="font-extrabold">{desafio.enunciado}</p>
                        <p className="text-sm">
                          {desafio.licao} · {desafio.respostas}{" "}
                          {desafio.respostas === 1 ? "primeira resposta" : "primeiras respostas"}
                        </p>
                      </div>
                      <span
                        className={`rounded-lg px-2 py-0.5 text-sm font-bold ${CALIBRACOES[desafio.calibracao].cor}`}
                      >
                        {CALIBRACOES[desafio.calibracao].rotulo}
                      </span>
                    </div>
                    <BarraDePercentual valor={desafio.acerto} rotulo={`Acerto em: ${desafio.enunciado}`} />
                    <p className="text-sm">
                      Cadastrado como {DIFICULDADES[desafio.dificuldade]}
                      {desafio.dificuldadeObservada
                        ? `; na prática está ${DIFICULDADES[desafio.dificuldadeObservada]}`
                        : ""}
                      .
                    </p>
                    {desafio.erroMaisComum && (
                      <p className="text-sm">
                        Erro mais comum: <strong>{desafio.erroMaisComum}</strong> (
                        {desafio.vezesDoErroMaisComum} {desafio.vezesDoErroMaisComum === 1 ? "vez" : "vezes"})
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Secao>
        </>
      )}
    </div>
  );
}

function AbaDeEngajamento() {
  const [semanas, setSemanas] = useState(12);
  const engajamento = useEngajamento(semanas);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <Campo rotulo="Período">
          <select
            value={semanas}
            onChange={(evento) => setSemanas(Number(evento.target.value))}
            className={ESTILO_DA_SELECAO}
          >
            {SEMANAS.map((quantidade) => (
              <option key={quantidade} value={quantidade}>
                Últimas {quantidade} semanas
              </option>
            ))}
          </select>
        </Campo>
        <BotoesDeExportacao relatorio="engajamento" rotulo="o engajamento" valores={{ semanas }} />
      </div>
      {engajamento.isPending && <Carregando />}
      {engajamento.isError && <MensagemDeErro erro={engajamento.error} />}
      {engajamento.data && (
        <>
          <GraficoDeColunas
            rotulo={`Pessoas que concluíram ao menos uma lição, de ${engajamento.data.pessoasNoEscopo} ativas`}
            colunas={engajamento.data.semanas.map((semana) => ({
              rotulo: dataCurta(semana.inicio),
              valor: semana.pessoasAtivas,
              detalhe: `${semana.pessoasAtivas} pessoas, ${formatarPercentual(semana.participacao)}`,
            }))}
          />
          <div className="overflow-x-auto">
            <table className="w-full min-w-[40rem] text-left">
              <caption className="sr-only">Engajamento por semana</caption>
              <thead>
                <tr className="border-b-2 border-neutro">
                  <th className="p-2">Semana de</th>
                  <th className="p-2">Pessoas ativas</th>
                  <th className="p-2">Participação</th>
                  <th className="p-2">Lições concluídas</th>
                  <th className="p-2">Novos participantes</th>
                </tr>
              </thead>
              <tbody>
                {engajamento.data.semanas.map((semana) => (
                  <tr key={semana.inicio} className="border-b border-neutro">
                    <td className="p-2">{dataCurta(semana.inicio)}</td>
                    <td className="p-2">{semana.pessoasAtivas}</td>
                    <td className="p-2">{formatarPercentual(semana.participacao)}</td>
                    <td className="p-2">{semana.licoesConcluidas}</td>
                    <td className="p-2">{semana.novosParticipantes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

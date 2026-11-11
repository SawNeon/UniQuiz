import { useRef, useState } from "react";
import { useSalvarDesafio } from "../../api/admin";
import type {
  ConteudoAdmin,
  ConteudoCompletarClicandoAdmin,
  ConteudoMultiplaEscolhaAdmin,
  ConteudoVerdadeiroOuFalsoAdmin,
  DadosDoDesafio,
  DesafioParaAdmin,
  Dificuldade,
  RespostaDoColaborador,
  TipoDesafio,
} from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { ComponenteDoDesafio, respostaCompleta } from "../desafios/ComponenteDoDesafio";
import { AreaDeTexto, Campo, Entrada } from "./componentes";
import { desfazerLacuna, transformarEmLacuna } from "./lacunas";
import { acertouNaPrevia, paraPublico } from "./previa";

const TIPOS: Record<TipoDesafio, string> = {
  MULTIPLA_ESCOLHA: "Múltipla escolha",
  VERDADEIRO_OU_FALSO: "Verdadeiro ou falso",
  COMPLETAR_CLICANDO: "Completar clicando",
};

const DIFICULDADES: Record<Dificuldade, string> = { FACIL: "Fácil", MEDIO: "Médio", DIFICIL: "Difícil" };

function conteudoInicial(tipo: TipoDesafio): ConteudoAdmin {
  switch (tipo) {
    case "MULTIPLA_ESCOLHA":
      return {
        tipo,
        conteudo: {
          alternativas: [
            { id: "a", texto: "", correta: true, motivo: "" },
            { id: "b", texto: "", correta: false, motivo: "" },
          ],
        },
      };
    case "VERDADEIRO_OU_FALSO":
      return { tipo, conteudo: { afirmacao: "", verdadeira: true, motivoDoErro: "" } };
    case "COMPLETAR_CLICANDO":
      return { tipo, conteudo: { texto: "", lacunas: [] } };
  }
}

function dadosIniciais(desafio?: DesafioParaAdmin): DadosDoDesafio {
  if (desafio) {
    const { enunciado, dificuldade, pontos, explicacao, dica } = desafio;
    const conteudo = { tipo: desafio.tipo, conteudo: desafio.conteudo } as ConteudoAdmin;
    return { enunciado, dificuldade, pontos, explicacao, dica, ...conteudo };
  }
  return {
    enunciado: "",
    dificuldade: "FACIL",
    pontos: 10,
    explicacao: "",
    dica: "",
    ...conteudoInicial("MULTIPLA_ESCOLHA"),
  };
}

export function EditorDeDesafio({
  licaoId,
  desafio,
  aoTerminar,
}: {
  licaoId: number;
  desafio?: DesafioParaAdmin;
  aoTerminar: () => void;
}) {
  const [dados, setDados] = useState<DadosDoDesafio>(() => dadosIniciais(desafio));
  const salvar = useSalvarDesafio(licaoId);

  function atualizar(parcial: Partial<Pick<DadosDoDesafio, "enunciado" | "dificuldade" | "pontos" | "explicacao" | "dica">>) {
    setDados((atual) => ({ ...atual, ...parcial }));
  }

  function trocarConteudo(conteudo: ConteudoAdmin) {
    setDados((atual) => ({ ...atual, ...conteudo }) as DadosDoDesafio);
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <form
        className="flex flex-col gap-4"
        onSubmit={(evento) => {
          evento.preventDefault();
          salvar.mutate({ id: desafio?.id, dados }, { onSuccess: aoTerminar });
        }}
      >
        {desafio?.status === "PUBLICADO" && (
          <p className="rounded-padrao bg-info p-3 text-sm font-bold">
            Este desafio está publicado. Salvar cria a versão {desafio.versao + 1}; as respostas antigas continuam
            ligadas à versão que a pessoa viu.
          </p>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Campo rotulo="Tipo">
            <select
              value={dados.tipo}
              disabled={Boolean(desafio)}
              onChange={(evento) => trocarConteudo(conteudoInicial(evento.target.value as TipoDesafio))}
              className="min-h-11 rounded-xl border-2 border-neutro bg-white px-2"
            >
              {Object.entries(TIPOS).map(([valor, rotulo]) => (
                <option key={valor} value={valor}>
                  {rotulo}
                </option>
              ))}
            </select>
          </Campo>
          <Campo rotulo="Dificuldade">
            <select
              value={dados.dificuldade}
              onChange={(evento) => atualizar({ dificuldade: evento.target.value as Dificuldade })}
              className="min-h-11 rounded-xl border-2 border-neutro bg-white px-2"
            >
              {Object.entries(DIFICULDADES).map(([valor, rotulo]) => (
                <option key={valor} value={valor}>
                  {rotulo}
                </option>
              ))}
            </select>
          </Campo>
        </div>
        <Campo rotulo="Enunciado">
          <Entrada value={dados.enunciado} onChange={(evento) => atualizar({ enunciado: evento.target.value })} />
        </Campo>

        {dados.tipo === "MULTIPLA_ESCOLHA" && (
          <EditorDeMultiplaEscolha
            conteudo={dados.conteudo}
            aoMudar={(conteudo) => trocarConteudo({ tipo: "MULTIPLA_ESCOLHA", conteudo })}
          />
        )}
        {dados.tipo === "VERDADEIRO_OU_FALSO" && (
          <EditorDeVerdadeiroOuFalso
            conteudo={dados.conteudo}
            aoMudar={(conteudo) => trocarConteudo({ tipo: "VERDADEIRO_OU_FALSO", conteudo })}
          />
        )}
        {dados.tipo === "COMPLETAR_CLICANDO" && (
          <SeletorDeLacunas
            conteudo={dados.conteudo}
            aoMudar={(conteudo) => trocarConteudo({ tipo: "COMPLETAR_CLICANDO", conteudo })}
          />
        )}

        <Campo rotulo="Explicação geral (aparece depois de responder)">
          <AreaDeTexto
            rows={3}
            value={dados.explicacao}
            onChange={(evento) => atualizar({ explicacao: evento.target.value })}
          />
        </Campo>
        <div className="grid grid-cols-[1fr_8rem] gap-3">
          <Campo rotulo="Dica (opcional)">
            <Entrada value={dados.dica ?? ""} onChange={(evento) => atualizar({ dica: evento.target.value })} />
          </Campo>
          <Campo rotulo="Pontos">
            <Entrada
              type="number"
              min={1}
              max={100}
              value={dados.pontos}
              onChange={(evento) => atualizar({ pontos: Number(evento.target.value) })}
            />
          </Campo>
        </div>
        {salvar.isError && <MensagemDeErro erro={salvar.error} />}
        <div className="flex gap-3">
          <Botao type="submit" disabled={salvar.isPending}>
            Salvar desafio
          </Botao>
          <Botao variante="secundario" onClick={aoTerminar}>
            Cancelar
          </Botao>
        </div>
      </form>

      <Previa dados={dados} />
    </div>
  );
}

function EditorDeMultiplaEscolha({
  conteudo,
  aoMudar,
}: {
  conteudo: ConteudoMultiplaEscolhaAdmin;
  aoMudar: (conteudo: ConteudoMultiplaEscolhaAdmin) => void;
}) {
  const alternativas = conteudo.alternativas;

  function mudar(indice: number, parcial: Partial<(typeof alternativas)[number]>) {
    aoMudar({
      alternativas: alternativas.map((alternativa, posicao) => {
        if (parcial.correta) {
          return { ...alternativa, correta: posicao === indice, ...(posicao === indice ? parcial : {}) };
        }
        return posicao === indice ? { ...alternativa, ...parcial } : alternativa;
      }),
    });
  }

  function adicionar() {
    const usados = new Set(alternativas.map((alternativa) => alternativa.id));
    const id = "abcdef".split("").find((letra) => !usados.has(letra));
    if (id) {
      aoMudar({ alternativas: [...alternativas, { id, texto: "", correta: false, motivo: "" }] });
    }
  }

  return (
    <fieldset className="flex flex-col gap-3 rounded-padrao border-2 border-neutro p-3">
      <legend className="px-1 font-extrabold">Alternativas (marque a certa e explique as erradas)</legend>
      {alternativas.map((alternativa, indice) => (
        <div key={alternativa.id} className="flex flex-col gap-2 rounded-xl bg-neutro/50 p-3">
          <div className="flex items-center gap-2">
            <input
              type="radio"
              name="alternativa-correta"
              aria-label={`Alternativa ${alternativa.id.toUpperCase()} é a certa`}
              checked={alternativa.correta}
              onChange={() => mudar(indice, { correta: true })}
              className="size-6 accent-[#00995d]"
            />
            <Entrada
              aria-label={`Texto da alternativa ${alternativa.id.toUpperCase()}`}
              value={alternativa.texto}
              onChange={(evento) => mudar(indice, { texto: evento.target.value })}
              className="flex-1"
            />
            <button
              type="button"
              aria-label={`Remover a alternativa ${alternativa.id.toUpperCase()}`}
              disabled={alternativas.length <= 2}
              onClick={() => aoMudar({ alternativas: alternativas.filter((_, posicao) => posicao !== indice) })}
              className="size-11 rounded-xl bg-white font-extrabold disabled:opacity-40"
            >
              ×
            </button>
          </div>
          {!alternativa.correta && (
            <Entrada
              aria-label={`Por que a alternativa ${alternativa.id.toUpperCase()} está errada`}
              placeholder="Por que esta alternativa está errada?"
              value={alternativa.motivo}
              onChange={(evento) => mudar(indice, { motivo: evento.target.value })}
            />
          )}
        </div>
      ))}
      {alternativas.length < 6 && (
        <Botao variante="secundario" onClick={adicionar}>
          Adicionar alternativa
        </Botao>
      )}
    </fieldset>
  );
}

function EditorDeVerdadeiroOuFalso({
  conteudo,
  aoMudar,
}: {
  conteudo: ConteudoVerdadeiroOuFalsoAdmin;
  aoMudar: (conteudo: ConteudoVerdadeiroOuFalsoAdmin) => void;
}) {
  return (
    <fieldset className="flex flex-col gap-3 rounded-padrao border-2 border-neutro p-3">
      <legend className="px-1 font-extrabold">Afirmação</legend>
      <AreaDeTexto
        aria-label="Afirmação"
        rows={2}
        value={conteudo.afirmacao}
        onChange={(evento) => aoMudar({ ...conteudo, afirmacao: evento.target.value })}
      />
      <Campo rotulo="A afirmação é">
        <select
          value={String(conteudo.verdadeira)}
          onChange={(evento) => aoMudar({ ...conteudo, verdadeira: evento.target.value === "true" })}
          className="min-h-11 rounded-xl border-2 border-neutro bg-white px-2"
        >
          <option value="true">Verdadeira</option>
          <option value="false">Falsa</option>
        </select>
      </Campo>
      <Campo rotulo="O que a pessoa precisa entender se errar">
        <AreaDeTexto
          rows={2}
          value={conteudo.motivoDoErro}
          onChange={(evento) => aoMudar({ ...conteudo, motivoDoErro: evento.target.value })}
        />
      </Campo>
    </fieldset>
  );
}

function SeletorDeLacunas({
  conteudo,
  aoMudar,
}: {
  conteudo: ConteudoCompletarClicandoAdmin;
  aoMudar: (conteudo: ConteudoCompletarClicandoAdmin) => void;
}) {
  const campo = useRef<HTMLTextAreaElement>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  function criarLacuna() {
    const area = campo.current;
    if (!area) {
      return;
    }
    const resultado = transformarEmLacuna(conteudo, area.selectionStart, area.selectionEnd);
    if ("erro" in resultado) {
      setAviso(resultado.erro);
      return;
    }
    setAviso(null);
    aoMudar(resultado);
  }

  function mudarBanco(lacunaId: number, banco: ConteudoCompletarClicandoAdmin["lacunas"][number]["banco"]) {
    aoMudar({
      ...conteudo,
      lacunas: conteudo.lacunas.map((lacuna) => (lacuna.id === lacunaId ? { ...lacuna, banco } : lacuna)),
    });
  }

  return (
    <fieldset className="flex flex-col gap-3 rounded-padrao border-2 border-neutro p-3">
      <legend className="px-1 font-extrabold">Frase e lacunas</legend>
      <p className="text-sm">
        Escreva a frase completa, selecione com o mouse a palavra que vai faltar e toque em “Transformar em lacuna”.
      </p>
      <AreaDeTexto
        ref={campo}
        aria-label="Frase completa"
        rows={3}
        value={conteudo.texto}
        onChange={(evento) => aoMudar({ ...conteudo, texto: evento.target.value })}
      />
      <Botao variante="secundario" onMouseDown={(evento) => evento.preventDefault()} onClick={criarLacuna}>
        Transformar em lacuna
      </Botao>
      {aviso && (
        <p role="alert" className="rounded-xl bg-aviso p-2 text-sm font-bold">
          {aviso}
        </p>
      )}

      {conteudo.lacunas.map((lacuna) => {
        const certa = lacuna.banco.find((opcao) => opcao.correta);
        const erradas = lacuna.banco.filter((opcao) => !opcao.correta);
        return (
          <div key={lacuna.id} className="flex flex-col gap-2 rounded-xl bg-neutro/50 p-3">
            <div className="flex items-center justify-between gap-2">
              <span className="font-extrabold">
                Lacuna {lacuna.id}: “{certa?.palavra}”
              </span>
              <button
                type="button"
                className="min-h-11 px-2 text-sm font-bold underline"
                onClick={() => aoMudar(desfazerLacuna(conteudo, lacuna.id))}
              >
                Desfazer lacuna
              </button>
            </div>
            {erradas.map((opcao, indice) => (
              <div key={indice} className="grid grid-cols-[10rem_1fr_auto] gap-2">
                <Entrada
                  aria-label={`Palavra errada ${indice + 1} da lacuna ${lacuna.id}`}
                  value={opcao.palavra}
                  onChange={(evento) =>
                    mudarBanco(lacuna.id, [
                      ...(certa ? [certa] : []),
                      ...erradas.map((outra, posicao) =>
                        posicao === indice ? { ...outra, palavra: evento.target.value } : outra,
                      ),
                    ])
                  }
                />
                <Entrada
                  aria-label={`Por que a palavra ${indice + 1} da lacuna ${lacuna.id} está errada`}
                  placeholder="Por que está errada?"
                  value={opcao.motivo}
                  onChange={(evento) =>
                    mudarBanco(lacuna.id, [
                      ...(certa ? [certa] : []),
                      ...erradas.map((outra, posicao) =>
                        posicao === indice ? { ...outra, motivo: evento.target.value } : outra,
                      ),
                    ])
                  }
                />
                <button
                  type="button"
                  aria-label={`Remover a palavra errada ${indice + 1}`}
                  onClick={() =>
                    mudarBanco(lacuna.id, [
                      ...(certa ? [certa] : []),
                      ...erradas.filter((_, posicao) => posicao !== indice),
                    ])
                  }
                  className="size-11 rounded-xl bg-white font-extrabold"
                >
                  ×
                </button>
              </div>
            ))}
            <Botao
              variante="discreto"
              onClick={() => mudarBanco(lacuna.id, [...lacuna.banco, { palavra: "", correta: false, motivo: "" }])}
            >
              Adicionar palavra errada
            </Botao>
          </div>
        );
      })}
    </fieldset>
  );
}

function Previa({ dados }: { dados: DadosDoDesafio }) {
  const [resposta, setResposta] = useState<RespostaDoColaborador | null>(null);
  const [verificado, setVerificado] = useState<boolean | null>(null);
  const desafio = paraPublico(dados);

  return (
    <section aria-label="Pré-visualização do desafio" className="flex flex-col gap-4 rounded-padrao bg-neutro/40 p-5">
      <p className="text-sm font-extrabold tracking-widest">COMO O COLABORADOR VÊ (SEM GRAVAR PONTOS)</p>
      <h2 className="text-[26px] leading-tight font-extrabold">{dados.enunciado || "Enunciado do desafio"}</h2>
      {dados.dica && <p className="rounded-padrao bg-aviso p-3 text-sm font-bold">Dica: {dados.dica}</p>}
      <ComponenteDoDesafio
        desafio={desafio}
        resposta={resposta}
        aoResponder={(nova) => {
          setResposta(nova);
          setVerificado(null);
        }}
        correcao={verificado === null ? null : { correta: verificado }}
        desabilitado={false}
      />
      <Botao
        disabled={!respostaCompleta(desafio, resposta)}
        onClick={() => resposta && setVerificado(acertouNaPrevia(dados, resposta))}
      >
        Verificar
      </Botao>
      {verificado !== null && (
        <p role="status" className={`rounded-padrao p-3 font-bold ${verificado ? "bg-acerto-suave" : "bg-erro-suave"}`}>
          {verificado ? "Muito bem!" : "Quase!"} {dados.explicacao}
        </p>
      )}
    </section>
  );
}

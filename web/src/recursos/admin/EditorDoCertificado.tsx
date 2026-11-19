import { type KeyboardEvent, type PointerEvent, useEffect, useRef, useState } from "react";
import {
  enderecoDaArte,
  useBaixarExemplo,
  useEnviarArte,
  useModeloDoCertificado,
  useRemoverArte,
  useSalvarModelo,
} from "../../api/certificados";
import type {
  AlinhamentoDoCampo,
  CampoDoCertificado,
  ModeloDoCertificado,
  PosicaoDoCampo,
} from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";

const LARGURA_DA_PAGINA_EM_PONTOS = 841.89;

const CAMPOS_PADRAO: PosicaoDoCampo[] = [
  campo("NOME", 50, 41.2, 30, "#00995D", "CENTRO", true, 85, ""),
  campo("MODULO", 50, 55.4, 24, "#004E4C", "CENTRO", true, 85, ""),
  campo("DATA", 50, 68.9, 14, "#004E4C", "CENTRO", false, 60, ""),
  campo("CODIGO", 5.7, 89.9, 12, "#004E4C", "ESQUERDA", true, 50, "Código de validação: "),
  campo("ENDERECO_DE_VALIDACAO", 5.7, 92.9, 11, "#004E4C", "ESQUERDA", false, 70, "Confira em "),
];

const ROTULOS_DOS_CAMPOS: Record<CampoDoCertificado, string> = {
  NOME: "Nome da pessoa",
  MODULO: "Módulo",
  DATA: "Data de emissão",
  CODIGO: "Código de validação",
  ENDERECO_DE_VALIDACAO: "Endereço para validar",
};

const ALINHAMENTOS: { valor: AlinhamentoDoCampo; rotulo: string }[] = [
  { valor: "ESQUERDA", rotulo: "Esquerda" },
  { valor: "CENTRO", rotulo: "Centro" },
  { valor: "DIREITA", rotulo: "Direita" },
];

const CORES_DA_MARCA = [
  { cor: "#00995D", nome: "Verde Unimed" },
  { cor: "#004E4C", nome: "Verde escuro" },
  { cor: "#B1D34B", nome: "Verde-limão" },
  { cor: "#0A5F55", nome: "Verde petróleo" },
  { cor: "#5B5C65", nome: "Cinza" },
  { cor: "#FFFFFF", nome: "Branco" },
];

const DESLOCAMENTO: Record<AlinhamentoDoCampo, string> = {
  ESQUERDA: "0%",
  CENTRO: "-50%",
  DIREITA: "-100%",
};

function campo(
  nome: CampoDoCertificado,
  x: number,
  y: number,
  tamanho: number,
  cor: string,
  alinhamento: AlinhamentoDoCampo,
  negrito: boolean,
  larguraMaxima: number,
  prefixo: string,
): PosicaoDoCampo {
  return { campo: nome, visivel: true, x, y, tamanho, cor, alinhamento, negrito, larguraMaxima, prefixo };
}

function exemplos(): Record<CampoDoCertificado, string> {
  return {
    NOME: "Maria Aparecida Exemplo da Silva",
    MODULO: "Segurança da informação no dia a dia",
    DATA: new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date()),
    CODIGO: "EXMP-2345-ABCD",
    ENDERECO_DE_VALIDACAO: `${window.location.origin}/certificado/EXMP2345ABCD`,
  };
}

function limitar(valor: number, minimo: number, maximo: number): number {
  return Math.round(Math.min(maximo, Math.max(minimo, valor)) * 10) / 10;
}

export function EditorDoCertificado() {
  const modelo = useModeloDoCertificado();

  if (modelo.isPending) {
    return <Carregando />;
  }
  if (modelo.isError) {
    return <MensagemDeErro erro={modelo.error} />;
  }
  return <Editor modelo={modelo.data} />;
}

function Editor({ modelo }: { modelo: ModeloDoCertificado }) {
  const [campos, setCampos] = useState<PosicaoDoCampo[]>(modelo.campos);
  const [selecionado, setSelecionado] = useState<CampoDoCertificado>("NOME");
  const salvar = useSalvarModelo();
  const exemplo = useBaixarExemplo();
  const alterado = JSON.stringify(campos) !== JSON.stringify(modelo.campos);

  useEffect(() => {
    setCampos(modelo.campos);
  }, [modelo.campos]);

  const atual = campos.find((posicao) => posicao.campo === selecionado);

  function mudar(nome: CampoDoCertificado, mudanca: Partial<PosicaoDoCampo>) {
    salvar.reset();
    setCampos((anteriores) =>
      anteriores.map((posicao) => (posicao.campo === nome ? { ...posicao, ...mudanca } : posicao)),
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <ArteDeFundo modelo={modelo} />

      <div className="flex flex-col gap-2">
        <h3 className="text-xl font-extrabold">Posição dos campos</h3>
        <p className="max-w-prose">
          Arraste cada campo para o lugar certo na arte, ou escolha o campo e ajuste os números ao lado. As
          setas do teclado também movem o campo escolhido. O texto de exemplo mostra o tamanho que o nome mais
          longo pode ocupar.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <Pagina
          modelo={modelo}
          campos={campos}
          selecionado={selecionado}
          aoSelecionar={setSelecionado}
          aoMover={(nome, x, y) => mudar(nome, { x, y })}
        />
        {atual && (
          <PainelDoCampo
            posicao={atual}
            aoMudar={(mudanca) => mudar(atual.campo, mudanca)}
            aoEscolher={setSelecionado}
          />
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Botao disabled={!alterado || salvar.isPending} onClick={() => salvar.mutate(campos)}>
          Salvar posições
        </Botao>
        <Botao variante="secundario" disabled={exemplo.isPending} onClick={() => exemplo.mutate(campos)}>
          Baixar PDF de exemplo
        </Botao>
        <Botao
          variante="discreto"
          onClick={() => {
            salvar.reset();
            setCampos(CAMPOS_PADRAO);
          }}
        >
          Voltar às posições padrão
        </Botao>
        {alterado && <span className="font-bold">Há mudanças que ainda não foram salvas.</span>}
        {salvar.isSuccess && !alterado && (
          <span role="status" className="font-bold">
            Posições salvas. Os próximos certificados baixados já saem assim.
          </span>
        )}
      </div>
      {salvar.isError && <MensagemDeErro erro={salvar.error} />}
      {exemplo.isError && <MensagemDeErro erro={exemplo.error} />}
    </div>
  );
}

function ArteDeFundo({ modelo }: { modelo: ModeloDoCertificado }) {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const enviar = useEnviarArte();
  const remover = useRemoverArte();

  return (
    <section aria-labelledby="titulo-arte" className="flex flex-col gap-3 rounded-padrao bg-neutro/50 p-4">
      <h3 id="titulo-arte" className="text-xl font-extrabold">
        Arte de fundo
      </h3>
      <p className="max-w-prose">
        Peça à comunicação a arte em A4 deitado (297 × 210 mm), em PDF ou em PNG/JPG com 3508 × 2480 pixels,
        seguindo o padrão da marca e com espaço livre para o nome, o módulo, a data e o código. Até 10 MB.
      </p>
      <p className="font-bold">
        {modelo.temArte
          ? `Arte em uso: ${modelo.nomeDaArte ?? "arte enviada"}`
          : "Sem arte enviada: o certificado usa o fundo padrão do UniQuiz."}
      </p>
      <div className="flex flex-wrap items-end gap-3">
        <Campo rotulo="Arquivo da arte">
          <input
            type="file"
            accept=".png,.jpg,.jpeg,.pdf,image/png,image/jpeg,application/pdf"
            onChange={(evento) => {
              setArquivo(evento.target.files?.[0] ?? null);
              enviar.reset();
            }}
            className="min-h-11"
          />
        </Campo>
        <Botao disabled={!arquivo || enviar.isPending} onClick={() => arquivo && enviar.mutate(arquivo)}>
          {enviar.isPending ? "Enviando…" : "Enviar arte"}
        </Botao>
        {modelo.temArte && (
          <Botao variante="secundario" disabled={remover.isPending} onClick={() => remover.mutate()}>
            Remover arte
          </Botao>
        )}
      </div>
      {enviar.isSuccess && (
        <div role="status" className="flex flex-col gap-1">
          <p className="font-bold">Arte enviada. Confira a posição dos campos abaixo.</p>
          {enviar.data.avisos.map((aviso) => (
            <p key={aviso} className="rounded-xl bg-aviso p-2 font-bold">
              {aviso}
            </p>
          ))}
        </div>
      )}
      {enviar.isError && <MensagemDeErro erro={enviar.error} />}
      {remover.isError && <MensagemDeErro erro={remover.error} />}
    </section>
  );
}

function Pagina({
  modelo,
  campos,
  selecionado,
  aoSelecionar,
  aoMover,
}: {
  modelo: ModeloDoCertificado;
  campos: PosicaoDoCampo[];
  selecionado: CampoDoCertificado;
  aoSelecionar: (campo: CampoDoCertificado) => void;
  aoMover: (campo: CampoDoCertificado, x: number, y: number) => void;
}) {
  const pagina = useRef<HTMLDivElement>(null);
  const arrasto = useRef<{ campo: CampoDoCertificado; dx: number; dy: number } | null>(null);
  const textos = exemplos();

  function pontoNaPagina(evento: PointerEvent): { x: number; y: number } {
    const caixa = pagina.current?.getBoundingClientRect();
    if (!caixa || caixa.width === 0 || caixa.height === 0) {
      return { x: 0, y: 0 };
    }
    return {
      x: ((evento.clientX - caixa.left) / caixa.width) * 100,
      y: ((evento.clientY - caixa.top) / caixa.height) * 100,
    };
  }

  function comecar(evento: PointerEvent<HTMLButtonElement>, posicao: PosicaoDoCampo) {
    aoSelecionar(posicao.campo);
    const ponto = pontoNaPagina(evento);
    arrasto.current = { campo: posicao.campo, dx: posicao.x - ponto.x, dy: posicao.y - ponto.y };
    evento.currentTarget.setPointerCapture?.(evento.pointerId);
  }

  function mover(evento: PointerEvent<HTMLButtonElement>) {
    if (!arrasto.current) {
      return;
    }
    const ponto = pontoNaPagina(evento);
    aoMover(
      arrasto.current.campo,
      limitar(ponto.x + arrasto.current.dx, 0, 100),
      limitar(ponto.y + arrasto.current.dy, 0, 100),
    );
  }

  function teclar(evento: KeyboardEvent<HTMLButtonElement>, posicao: PosicaoDoCampo) {
    const passo = evento.shiftKey ? 2 : 0.5;
    const direcoes: Record<string, [number, number]> = {
      ArrowLeft: [-passo, 0],
      ArrowRight: [passo, 0],
      ArrowUp: [0, -passo],
      ArrowDown: [0, passo],
    };
    const direcao = direcoes[evento.key];
    if (!direcao) {
      return;
    }
    evento.preventDefault();
    aoMover(posicao.campo, limitar(posicao.x + direcao[0], 0, 100), limitar(posicao.y + direcao[1], 0, 100));
  }

  return (
    <div
      ref={pagina}
      aria-label="Prévia do certificado"
      role="group"
      className="relative aspect-[297/210] w-full touch-none overflow-hidden rounded-xl border-2 border-neutro bg-white bg-cover bg-center select-none [container-type:inline-size]"
      style={
        modelo.temArte ? { backgroundImage: `url("${enderecoDaArte(modelo.atualizadoEm)}")` } : undefined
      }
    >
      {!modelo.temArte && <FundoPadrao />}
      {campos
        .filter((posicao) => posicao.visivel)
        .map((posicao) => (
          <button
            key={posicao.campo}
            type="button"
            aria-label={`${ROTULOS_DOS_CAMPOS[posicao.campo]}: arraste para mover`}
            aria-pressed={selecionado === posicao.campo}
            onPointerDown={(evento) => comecar(evento, posicao)}
            onPointerMove={mover}
            onPointerUp={() => {
              arrasto.current = null;
            }}
            onPointerCancel={() => {
              arrasto.current = null;
            }}
            onKeyDown={(evento) => teclar(evento, posicao)}
            onFocus={() => aoSelecionar(posicao.campo)}
            className={`absolute cursor-move overflow-hidden leading-none whitespace-nowrap ${
              selecionado === posicao.campo
                ? "outline-2 outline-offset-2 outline-dashed outline-[#f47920]"
                : ""
            }`}
            style={{
              left: `${posicao.x}%`,
              top: `${posicao.y}%`,
              maxWidth: `${posicao.larguraMaxima}%`,
              transform: `translate(${DESLOCAMENTO[posicao.alinhamento]}, -80%)`,
              color: posicao.cor,
              fontFamily: "Helvetica, Arial, sans-serif",
              fontWeight: posicao.negrito ? 700 : 400,
              fontSize: `${(posicao.tamanho / LARGURA_DA_PAGINA_EM_PONTOS) * 100}cqw`,
              textOverflow: "ellipsis",
            }}
          >
            {posicao.prefixo}
            {textos[posicao.campo]}
          </button>
        ))}
    </div>
  );
}

function FundoPadrao() {
  const texto = (topo: number, tamanho: number, conteudo: string, negrito = false) => (
    <span
      aria-hidden
      className="absolute inset-x-0 text-center leading-none text-[#004E4C]"
      style={{
        top: `${topo}%`,
        fontSize: `${(tamanho / LARGURA_DA_PAGINA_EM_PONTOS) * 100}cqw`,
        fontWeight: negrito ? 700 : 400,
        fontFamily: "Helvetica, Arial, sans-serif",
        transform: "translateY(-80%)",
      }}
    >
      {conteudo}
    </span>
  );
  return (
    <>
      <span aria-hidden className="absolute inset-x-0 top-0 h-[11.8%] bg-[#00995D]" />
      <span aria-hidden className="absolute inset-x-0 bottom-0 h-[3%] bg-[#B1D34B]" />
      {texto(25.2, 34, "Certificado de conclusão", true)}
      {texto(33.6, 16, "Certificamos que")}
      {texto(48.7, 16, "concluiu todas as lições do módulo")}
      {texto(62.2, 14, "no UniQuiz, programa de aprendizagem da equipe de TI.")}
    </>
  );
}

function PainelDoCampo({
  posicao,
  aoMudar,
  aoEscolher,
}: {
  posicao: PosicaoDoCampo;
  aoMudar: (mudanca: Partial<PosicaoDoCampo>) => void;
  aoEscolher: (campo: CampoDoCertificado) => void;
}) {
  const numero = (valor: string, minimo: number, maximo: number) =>
    limitar(Number(valor) || 0, minimo, maximo);

  return (
    <div className="flex flex-col gap-4 rounded-padrao bg-neutro/50 p-4">
      <Campo rotulo="Campo">
        <select
          value={posicao.campo}
          onChange={(evento) => aoEscolher(evento.target.value as CampoDoCertificado)}
          className="min-h-11 rounded-xl border-2 border-neutro bg-white px-2"
        >
          {Object.entries(ROTULOS_DOS_CAMPOS).map(([valor, rotulo]) => (
            <option key={valor} value={valor}>
              {rotulo}
            </option>
          ))}
        </select>
      </Campo>
      <label className="flex min-h-11 items-center gap-2 font-bold">
        <input
          type="checkbox"
          checked={posicao.visivel}
          onChange={(evento) => aoMudar({ visivel: evento.target.checked })}
          className="size-5"
        />
        Mostrar no certificado
      </label>
      <Campo rotulo="Texto antes do campo">
        <Entrada
          value={posicao.prefixo}
          maxLength={80}
          placeholder="Ex.: Varginha, "
          onChange={(evento) => aoMudar({ prefixo: evento.target.value })}
        />
      </Campo>
      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Horizontal (%)">
          <Entrada
            type="number"
            step={0.1}
            min={0}
            max={100}
            value={posicao.x}
            onChange={(evento) => aoMudar({ x: numero(evento.target.value, 0, 100) })}
          />
        </Campo>
        <Campo rotulo="Vertical (%)">
          <Entrada
            type="number"
            step={0.1}
            min={0}
            max={100}
            value={posicao.y}
            onChange={(evento) => aoMudar({ y: numero(evento.target.value, 0, 100) })}
          />
        </Campo>
        <Campo rotulo="Tamanho da letra">
          <Entrada
            type="number"
            step={1}
            min={6}
            max={96}
            value={posicao.tamanho}
            onChange={(evento) => aoMudar({ tamanho: numero(evento.target.value, 6, 96) })}
          />
        </Campo>
        <Campo rotulo="Largura máxima (%)">
          <Entrada
            type="number"
            step={1}
            min={5}
            max={100}
            value={posicao.larguraMaxima}
            onChange={(evento) => aoMudar({ larguraMaxima: numero(evento.target.value, 5, 100) })}
          />
        </Campo>
      </div>
      <fieldset className="flex flex-col gap-1.5">
        <legend className="font-extrabold">Alinhamento</legend>
        <div className="flex gap-2">
          {ALINHAMENTOS.map((item) => (
            <button
              key={item.valor}
              type="button"
              aria-pressed={posicao.alinhamento === item.valor}
              onClick={() => aoMudar({ alinhamento: item.valor })}
              className={`min-h-11 flex-1 rounded-xl px-2 font-bold ${
                posicao.alinhamento === item.valor ? "bg-primaria text-white" : "bg-white"
              }`}
            >
              {item.rotulo}
            </button>
          ))}
        </div>
      </fieldset>
      <label className="flex min-h-11 items-center gap-2 font-bold">
        <input
          type="checkbox"
          checked={posicao.negrito}
          onChange={(evento) => aoMudar({ negrito: evento.target.checked })}
          className="size-5"
        />
        Negrito
      </label>
      <fieldset className="flex flex-col gap-1.5">
        <legend className="font-extrabold">Cor</legend>
        <div className="flex flex-wrap gap-2">
          {CORES_DA_MARCA.map((item) => (
            <button
              key={item.cor}
              type="button"
              title={item.nome}
              aria-label={item.nome}
              aria-pressed={posicao.cor.toUpperCase() === item.cor}
              onClick={() => aoMudar({ cor: item.cor })}
              className={`size-11 rounded-xl border-2 ${
                posicao.cor.toUpperCase() === item.cor ? "border-texto" : "border-neutro"
              }`}
              style={{ backgroundColor: item.cor }}
            />
          ))}
          <input
            type="color"
            aria-label="Outra cor"
            value={posicao.cor.toLowerCase()}
            onChange={(evento) => aoMudar({ cor: evento.target.value.toUpperCase() })}
            className="size-11 rounded-xl"
          />
        </div>
      </fieldset>
    </div>
  );
}

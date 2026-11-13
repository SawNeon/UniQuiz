import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import {
  LETRAS_MINIMAS_NA_BUSCA,
  letrasDe,
  useColaboradoresDoSetor,
  useEmpresas,
  useEntrar,
  useSetores,
} from "../../api/consultas";
import type { ColaboradorDaEntrada, ItemDaLista } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Mascote } from "../../mascote/Mascote";
import { pinCompleto, pinDigitado, TAMANHO_DO_PIN } from "./pin";

const CHAVE_DA_EMPRESA_LEMBRADA = "uniquiz.empresa";
const ESPERA_DA_BUSCA_MS = 300;

function lerEmpresaLembrada(): number | null {
  try {
    const valor = window.localStorage.getItem(CHAVE_DA_EMPRESA_LEMBRADA);
    return valor ? Number(valor) : null;
  } catch {
    return null;
  }
}

function lembrarEmpresa(empresaId: number | null) {
  try {
    if (empresaId === null) {
      window.localStorage.removeItem(CHAVE_DA_EMPRESA_LEMBRADA);
    } else {
      window.localStorage.setItem(CHAVE_DA_EMPRESA_LEMBRADA, String(empresaId));
    }
  } catch {
    return;
  }
}

function escolhaAutomatica(opcoes: ItemDaLista[] | undefined, preferida: number | null): number | null {
  if (!opcoes) {
    return null;
  }
  if (opcoes.length === 1) {
    return opcoes[0]?.id ?? null;
  }
  return opcoes.some((opcao) => opcao.id === preferida) ? preferida : null;
}

function useValorAtrasado(valor: string, espera: number): string {
  const [atrasado, setAtrasado] = useState(valor);
  useEffect(() => {
    const temporizador = window.setTimeout(() => setAtrasado(valor), espera);
    return () => window.clearTimeout(temporizador);
  }, [valor, espera]);
  return atrasado;
}

export function TelaDeEntrada() {
  const navegar = useNavigate();
  const [empresaEscolhida, setEmpresaEscolhida] = useState<number | null | undefined>(undefined);
  const [setorEscolhido, setSetorEscolhido] = useState<number | null | undefined>(undefined);
  const [busca, setBusca] = useState("");
  const [escolhido, setEscolhido] = useState<ColaboradorDaEntrada | null>(null);

  const empresas = useEmpresas();
  const empresaId =
    empresaEscolhida === undefined
      ? escolhaAutomatica(empresas.data, lerEmpresaLembrada())
      : empresaEscolhida;
  const setores = useSetores(empresaId);
  const setorId = setorEscolhido === undefined ? escolhaAutomatica(setores.data, null) : setorEscolhido;
  const buscaAtrasada = useValorAtrasado(busca, ESPERA_DA_BUSCA_MS);
  const colaboradores = useColaboradoresDoSetor(setorId, buscaAtrasada);
  const nomeDoSetor = setores.data?.find((setor) => setor.id === setorId)?.nome ?? "";
  const buscaCurta = letrasDe(busca.trim()) < LETRAS_MINIMAS_NA_BUSCA;

  function escolherEmpresa(id: number | null) {
    setEmpresaEscolhida(id);
    setSetorEscolhido(undefined);
    setBusca("");
    lembrarEmpresa(id);
  }

  function escolherSetor(id: number | null) {
    setSetorEscolhido(id);
    setBusca("");
  }

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col gap-6 px-4 py-8">
      <header className="flex items-center gap-4">
        <Mascote estado="feliz" tamanho={88} />
        <div>
          <h1 className="text-4xl font-extrabold">UniQuiz</h1>
          <p className="text-lg font-bold">Desafios da TI</p>
        </div>
      </header>

      {escolhido ? (
        <ConfirmacaoDeIdentidade
          pessoa={escolhido}
          setor={nomeDoSetor}
          aoVoltar={() => setEscolhido(null)}
          aoEntrar={() => void navegar("/", { replace: true })}
        />
      ) : (
        <form className="flex flex-col gap-5" onSubmit={(evento) => evento.preventDefault()}>
          <div className="flex flex-col gap-2">
            <label htmlFor="empresa" className="text-lg font-extrabold">
              Empresa
            </label>
            <select
              id="empresa"
              className="min-h-12 rounded-padrao border-2 border-neutro bg-white px-3 text-lg"
              value={empresaId ?? ""}
              onChange={(evento) => escolherEmpresa(evento.target.value ? Number(evento.target.value) : null)}
            >
              <option value="">Escolha a empresa</option>
              {empresas.data?.map((empresa) => (
                <option key={empresa.id} value={empresa.id}>
                  {empresa.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="setor" className="text-lg font-extrabold">
              Setor
            </label>
            <select
              id="setor"
              disabled={empresaId === null}
              className="min-h-12 rounded-padrao border-2 border-neutro bg-white px-3 text-lg disabled:opacity-60"
              value={setorId ?? ""}
              onChange={(evento) => escolherSetor(evento.target.value ? Number(evento.target.value) : null)}
            >
              <option value="">Escolha o setor</option>
              {setores.data?.map((setor) => (
                <option key={setor.id} value={setor.id}>
                  {setor.nome}
                </option>
              ))}
            </select>
          </div>

          {setorId !== null && (
            <div className="flex flex-col gap-2">
              <label htmlFor="busca" className="text-lg font-extrabold">
                Seu nome
              </label>
              <input
                id="busca"
                type="search"
                autoComplete="off"
                aria-describedby="dica-da-busca"
                placeholder="Digite parte do seu nome"
                value={busca}
                onChange={(evento) => setBusca(evento.target.value)}
                className="min-h-12 rounded-padrao border-2 border-neutro px-3 text-lg"
              />
              <p id="dica-da-busca" className="text-sm">
                Por privacidade, os nomes só aparecem depois de {LETRAS_MINIMAS_NA_BUSCA} letras.
              </p>
              {!buscaCurta && (
                <ResultadoDaBusca
                  carregando={colaboradores.isFetching}
                  erro={colaboradores.error}
                  pessoas={colaboradores.data}
                  aoEscolher={setEscolhido}
                />
              )}
            </div>
          )}

          {empresas.isError && (
            <MensagemDeErro erro={empresas.error} aoTentarDeNovo={() => void empresas.refetch()} />
          )}
        </form>
      )}

      <p className="mt-auto text-sm">
        Guardamos seu nome, setor, empresa e pontos para montar a classificação e emitir seus certificados.
      </p>
    </main>
  );
}

function ResultadoDaBusca({
  carregando,
  erro,
  pessoas,
  aoEscolher,
}: {
  carregando: boolean;
  erro: unknown;
  pessoas: ColaboradorDaEntrada[] | undefined;
  aoEscolher: (pessoa: ColaboradorDaEntrada) => void;
}) {
  if (erro) {
    return <MensagemDeErro erro={erro} />;
  }
  if (!pessoas) {
    return carregando ? <p role="status">Procurando…</p> : null;
  }
  return (
    <ul aria-label="Nomes encontrados" className="flex flex-col gap-2">
      {pessoas.map((pessoa) => (
        <li key={pessoa.id}>
          <button
            type="button"
            onClick={() => aoEscolher(pessoa)}
            className="min-h-12 w-full rounded-padrao bg-neutro px-4 text-left text-lg font-bold hover:bg-acerto-suave"
          >
            {pessoa.nome}
          </button>
        </li>
      ))}
      {pessoas.length === 0 && (
        <li className="p-2">
          Nenhum nome encontrado. Confira a empresa e o setor ou procure o administrador do UniQuiz.
        </li>
      )}
    </ul>
  );
}

function ConfirmacaoDeIdentidade({
  pessoa,
  setor,
  aoVoltar,
  aoEntrar,
}: {
  pessoa: ColaboradorDaEntrada;
  setor: string;
  aoVoltar: () => void;
  aoEntrar: () => void;
}) {
  const entrar = useEntrar();
  const [pin, setPin] = useState("");
  const [repeticao, setRepeticao] = useState("");
  const [aparelhoCompartilhado, setAparelhoCompartilhado] = useState(false);
  const primeiroAcesso = !pessoa.temPin;
  const pinsDiferentes = primeiroAcesso && pinCompleto(repeticao) && repeticao !== pin;
  const pronto = pinCompleto(pin) && (!primeiroAcesso || repeticao === pin);

  return (
    <form
      aria-labelledby="titulo-confirmacao"
      className="flex flex-col gap-4 rounded-padrao bg-acerto-suave p-5"
      onSubmit={(evento) => {
        evento.preventDefault();
        if (pronto) {
          entrar.mutate({ colaboradorId: pessoa.id, pin, aparelhoCompartilhado }, { onSuccess: aoEntrar });
        }
      }}
    >
      <h2 id="titulo-confirmacao" className="text-2xl font-extrabold">
        Você é {pessoa.nome}, do setor {setor}?
      </h2>
      <p>
        {primeiroAcesso
          ? "É o seu primeiro acesso. Crie um PIN de 3 números e guarde: você vai usar nas próximas vezes."
          : "Digite o seu PIN de 3 números."}
      </p>
      <CampoDePin id="pin" rotulo={primeiroAcesso ? "Crie o seu PIN" : "PIN"} valor={pin} aoMudar={setPin} />
      {primeiroAcesso && (
        <CampoDePin id="repeticao" rotulo="Repita o PIN" valor={repeticao} aoMudar={setRepeticao} />
      )}
      {pinsDiferentes && (
        <p role="alert" className="font-bold">
          Os dois PINs precisam ser iguais.
        </p>
      )}
      {!primeiroAcesso && <p className="text-sm">Esqueceu o PIN? Procure o administrador do UniQuiz.</p>}
      <label className="flex min-h-11 items-start gap-3">
        <input
          type="checkbox"
          className="mt-1 size-5"
          checked={aparelhoCompartilhado}
          onChange={(evento) => setAparelhoCompartilhado(evento.target.checked)}
        />
        <span>
          <span className="font-bold">Estou em um aparelho compartilhado</span>
          <span className="block text-sm">Você sai sozinho depois de 15 minutos sem uso.</span>
        </span>
      </label>
      {entrar.isError && <MensagemDeErro erro={entrar.error} />}
      <Botao larguraTotal type="submit" disabled={entrar.isPending || !pronto}>
        Sim, sou eu
      </Botao>
      <Botao variante="secundario" larguraTotal type="button" onClick={aoVoltar}>
        Não, voltar
      </Botao>
    </form>
  );
}

function CampoDePin({
  id,
  rotulo,
  valor,
  aoMudar,
}: {
  id: string;
  rotulo: string;
  valor: string;
  aoMudar: (valor: string) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-lg font-extrabold">
        {rotulo}
      </label>
      <input
        id={id}
        type="password"
        inputMode="numeric"
        autoComplete="off"
        maxLength={TAMANHO_DO_PIN}
        value={valor}
        onChange={(evento) => aoMudar(pinDigitado(evento.target.value))}
        className="min-h-12 w-32 rounded-padrao border-2 border-neutro bg-white px-3 text-center text-2xl tracking-[0.5em]"
      />
    </div>
  );
}

import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { useColaboradoresDoSetor, useEmpresas, useEntrar, useSetores } from "../../api/consultas";
import type { ColaboradorDaEntrada } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Mascote } from "../../mascote/Mascote";
import { pinCompleto, pinDigitado, TAMANHO_DO_PIN } from "./pin";

function semAcento(texto: string): string {
  return texto.normalize("NFD").replace(/\p{M}/gu, "").toLocaleLowerCase("pt-BR");
}

export function TelaDeEntrada() {
  const navegar = useNavigate();
  const [empresaId, setEmpresaId] = useState<number | null>(null);
  const [setorId, setSetorId] = useState<number | null>(null);
  const [busca, setBusca] = useState("");
  const [escolhido, setEscolhido] = useState<ColaboradorDaEntrada | null>(null);
  const [pin, setPin] = useState("");

  const empresas = useEmpresas();
  const setores = useSetores(empresaId);
  const colaboradores = useColaboradoresDoSetor(setorId);
  const entrar = useEntrar();

  const filtrados = useMemo(() => {
    const termo = semAcento(busca.trim());
    return (colaboradores.data ?? []).filter((pessoa) => semAcento(pessoa.nome).includes(termo));
  }, [busca, colaboradores.data]);

  const nomeDoSetor = setores.data?.find((setor) => setor.id === setorId)?.nome;

  function confirmar() {
    if (!escolhido || !pinCompleto(pin)) {
      return;
    }
    entrar.mutate(
      { colaboradorId: escolhido.id, pin },
      { onSuccess: () => void navegar("/", { replace: true }) },
    );
  }

  function voltar() {
    setEscolhido(null);
    setPin("");
    entrar.reset();
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
        <form
          aria-labelledby="titulo-confirmacao"
          className="flex flex-col gap-4 rounded-padrao bg-acerto-suave p-5"
          onSubmit={(evento) => {
            evento.preventDefault();
            confirmar();
          }}
        >
          <h2 id="titulo-confirmacao" className="text-2xl font-extrabold">
            Você é {escolhido.nome}, do setor {nomeDoSetor}?
          </h2>
          <p>
            {escolhido.temPin
              ? "Digite o seu PIN de 3 números."
              : "É o seu primeiro acesso. Crie um PIN de 3 números e guarde: você vai usar nas próximas vezes."}
          </p>
          <div className="flex flex-col gap-2">
            <label htmlFor="pin" className="text-lg font-extrabold">
              {escolhido.temPin ? "PIN" : "Crie o seu PIN"}
            </label>
            <input
              id="pin"
              type="password"
              inputMode="numeric"
              autoComplete="off"
              maxLength={TAMANHO_DO_PIN}
              value={pin}
              onChange={(evento) => setPin(pinDigitado(evento.target.value))}
              className="min-h-12 w-32 rounded-padrao border-2 border-neutro bg-white px-3 text-center text-2xl tracking-[0.5em]"
            />
          </div>
          {entrar.isError && <MensagemDeErro erro={entrar.error} />}
          <Botao larguraTotal type="submit" disabled={entrar.isPending || !pinCompleto(pin)}>
            Sim, sou eu
          </Botao>
          <Botao variante="secundario" larguraTotal type="button" onClick={voltar}>
            Não, voltar
          </Botao>
        </form>
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
              onChange={(evento) => {
                setEmpresaId(evento.target.value ? Number(evento.target.value) : null);
                setSetorId(null);
              }}
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
              onChange={(evento) => setSetorId(evento.target.value ? Number(evento.target.value) : null)}
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
                placeholder="Digite para buscar"
                value={busca}
                onChange={(evento) => setBusca(evento.target.value)}
                className="min-h-12 rounded-padrao border-2 border-neutro px-3 text-lg"
              />
              <ul
                aria-label="Colaboradores do setor"
                className="flex max-h-72 flex-col gap-2 overflow-y-auto"
              >
                {filtrados.map((pessoa) => (
                  <li key={pessoa.id}>
                    <button
                      type="button"
                      onClick={() => setEscolhido(pessoa)}
                      className="min-h-12 w-full rounded-padrao bg-neutro px-4 text-left text-lg font-bold hover:bg-acerto-suave"
                    >
                      {pessoa.nome}
                    </button>
                  </li>
                ))}
                {colaboradores.isSuccess && filtrados.length === 0 && (
                  <li className="p-2">Nenhum nome encontrado neste setor.</li>
                )}
              </ul>
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

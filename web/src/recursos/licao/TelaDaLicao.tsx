import { useMutation, useQueryClient } from "@tanstack/react-query";
import { type FormEvent, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router";
import { api } from "../../api/cliente";
import { chaves } from "../../api/consultas";
import type {
  Correcao,
  RespostaDoColaborador,
  ResultadoDaLicao as Resultado,
  TentativaIniciada,
} from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Icone } from "../../componentes/ui/Icone";
import { alternarSom, tocar, useSomLigado } from "../../sons/sons";
import { ComponenteDoDesafio } from "../desafios/ComponenteDoDesafio";
import { respostaCompleta } from "../desafios/respostaCompleta";
import { desafioAtual, totalDePassos, useEstadoDaLicao } from "./estadoDaLicao";
import { PainelDeCorrecao } from "./PainelDeCorrecao";
import { ResultadoDaLicao } from "./ResultadoDaLicao";

export function TelaDaLicao() {
  const licaoId = Number(useParams().licaoId);
  const navegar = useNavigate();
  const clienteDeConsultas = useQueryClient();
  const somLigado = useSomLigado();
  const estado = useEstadoDaLicao();
  const desafio = desafioAtual(estado);
  const iniciadaPara = useRef<number | null>(null);

  const iniciar = useMutation({
    mutationFn: () => api.post<TentativaIniciada>(`/licoes/${licaoId}/tentativas`),
    onSuccess: estado.iniciar,
  });

  const verificar = useMutation({
    mutationFn: (dados: { desafioId: number; resposta: RespostaDoColaborador }) =>
      api.post<Correcao>(`/tentativas/${estado.tentativa?.tentativaId}/respostas`, dados),
    onSuccess: (correcao) => {
      estado.registrarCorrecao(correcao);
      tocar(correcao.correta ? "acerto" : "erro");
    },
  });

  const concluir = useMutation({
    mutationFn: () => api.post<Resultado>(`/tentativas/${estado.tentativa?.tentativaId}/concluir`),
    onSuccess: (resultado) => {
      tocar(resultado.trofeusNovos.length > 0 ? "trofeu" : "conclusao");
      void clienteDeConsultas.invalidateQueries({ queryKey: chaves.caminho });
      void clienteDeConsultas.invalidateQueries({ queryKey: chaves.eu });
      void clienteDeConsultas.invalidateQueries({ queryKey: chaves.classificacao });
    },
  });

  const { mutate: iniciarTentativa } = iniciar;

  useEffect(() => {
    if (iniciadaPara.current !== licaoId) {
      iniciadaPara.current = licaoId;
      iniciarTentativa();
    }
  }, [licaoId, iniciarTentativa]);

  useEffect(() => () => useEstadoDaLicao.getState().limpar(), []);

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault();
    if (!desafio) {
      return;
    }
    if (estado.correcao) {
      if (estado.avancar() === "concluir") {
        concluir.mutate();
      }
      return;
    }
    if (estado.resposta && respostaCompleta(desafio, estado.resposta)) {
      verificar.mutate({ desafioId: desafio.id, resposta: estado.resposta });
    }
  }

  if (concluir.isSuccess) {
    return (
      <main className="mx-auto max-w-xl px-4">
        <ResultadoDaLicao resultado={concluir.data} />
      </main>
    );
  }

  const progresso = totalDePassos(estado) === 0 ? 0 : (estado.respondidos / totalDePassos(estado)) * 100;

  return (
    <main className="mx-auto flex min-h-dvh max-w-xl flex-col">
      <div className="flex h-16 items-center gap-3 px-4">
        <button
          type="button"
          aria-label="Sair da lição"
          onClick={() => void navegar("/")}
          className="flex size-11 items-center justify-center rounded-padrao"
        >
          <Icone nome="fechar" strokeWidth={2.5} />
        </button>
        <div
          role="progressbar"
          aria-label="Progresso da lição"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(progresso)}
          className="h-4 flex-1 overflow-hidden rounded-lg bg-neutro"
        >
          <div className="h-4 rounded-lg bg-primaria transition-[width]" style={{ width: `${progresso}%` }} />
        </div>
        <button
          type="button"
          onClick={alternarSom}
          aria-label={somLigado ? "Desligar o som" : "Ligar o som"}
          aria-pressed={somLigado}
          className="flex size-11 items-center justify-center rounded-padrao bg-neutro"
        >
          <Icone nome={somLigado ? "somLigado" : "somDesligado"} />
        </button>
      </div>

      {iniciar.isPending && <Carregando texto="Preparando os desafios…" />}
      {iniciar.isError && <MensagemDeErro erro={iniciar.error} aoTentarDeNovo={() => iniciar.mutate()} />}
      {concluir.isPending && <Carregando texto="Somando seus pontos…" />}
      {concluir.isError && <MensagemDeErro erro={concluir.error} aoTentarDeNovo={() => concluir.mutate()} />}

      {desafio && estado.tentativa && (
        <form onSubmit={aoEnviar} className="flex flex-1 flex-col">
          <div className="flex flex-col gap-5 px-6 pt-3 pb-6">
            <div className="flex flex-col gap-1.5">
              <span className="text-[13px] font-extrabold tracking-widest uppercase">
                {estado.tentativa.titulo}
                {estado.segundaRodada ? " · segunda chance" : ""}
              </span>
              <h1 className="text-[26px] leading-tight font-extrabold">{desafio.enunciado}</h1>
              {desafio.dica && (
                <p className="rounded-padrao bg-aviso p-3 text-sm font-bold">Dica: {desafio.dica}</p>
              )}
              {!estado.tentativa.valePontosIntegrais && (
                <p className="rounded-padrao bg-info p-3 text-sm font-bold">
                  Você já fez esta lição. Agora é para treinar, com menos pontos.
                </p>
              )}
            </div>
            <ComponenteDoDesafio
              key={`${desafio.id}-${estado.segundaRodada}`}
              desafio={desafio}
              resposta={estado.resposta}
              aoResponder={estado.responder}
              correcao={estado.correcao}
              desabilitado={estado.correcao !== null || verificar.isPending}
            />
            {verificar.isError && <MensagemDeErro erro={verificar.error} />}
          </div>

          <div className="sticky bottom-0 mt-auto" aria-live="polite">
            {estado.correcao && <PainelDeCorrecao correcao={estado.correcao} />}
            <div
              className={`p-6 pt-0 ${estado.correcao ? (estado.correcao.correta ? "bg-acerto-suave" : "bg-erro-suave") : ""}`}
            >
              <Botao
                type="submit"
                larguraTotal
                className="min-h-14"
                disabled={
                  !estado.correcao && (!respostaCompleta(desafio, estado.resposta) || verificar.isPending)
                }
              >
                {estado.correcao ? "Continuar" : "Verificar"}
              </Botao>
            </div>
          </div>
        </form>
      )}
    </main>
  );
}

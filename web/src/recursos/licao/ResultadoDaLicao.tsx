import { motion } from "framer-motion";
import { Link } from "react-router";
import { useEmitirCertificado, enderecoDoPdf } from "../../api/consultas";
import type { MotivoDosPontos, ResultadoDaLicao as Resultado } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Icone, IconeDoTrofeu } from "../../componentes/ui/Icone";
import { Mascote } from "../../mascote/Mascote";

const MOTIVOS: Record<MotivoDosPontos, string> = {
  ACERTOS: "Acertos",
  LICAO_PERFEITA: "Lição perfeita",
  PRIMEIRA_LICAO_DO_DIA: "Primeira lição do dia",
  AJUSTE_DO_ADMIN: "Ajuste",
};

function formatarTempo(ms: number): string {
  const segundos = Math.round(ms / 1000);
  const minutos = Math.floor(segundos / 60);
  return minutos > 0 ? `${minutos} min ${segundos % 60} s` : `${segundos} s`;
}

export function ResultadoDaLicao({ resultado }: { resultado: Resultado }) {
  const emitir = useEmitirCertificado();

  return (
    <div className="flex flex-col items-center gap-6 py-6 text-center">
      <motion.div
        initial={{ scale: 0.6, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ type: "spring" }}
      >
        <Mascote estado={resultado.perfeita ? "comemorando" : "feliz"} tamanho={140} />
      </motion.div>
      <h1 className="text-3xl font-extrabold">
        {resultado.perfeita ? "Lição perfeita!" : "Lição concluída!"}
      </h1>

      <dl className="grid w-full grid-cols-3 gap-3">
        <div className="rounded-padrao bg-neutro p-3">
          <dt className="text-sm font-bold">Pontos</dt>
          <dd className="font-numeros text-3xl font-bold">+{resultado.pontosGanhos}</dd>
        </div>
        <div className="rounded-padrao bg-neutro p-3">
          <dt className="text-sm font-bold">Acertos</dt>
          <dd className="font-numeros text-3xl font-bold">
            {resultado.acertos}/{resultado.total}
          </dd>
        </div>
        <div className="rounded-padrao bg-neutro p-3">
          <dt className="text-sm font-bold">Tempo</dt>
          <dd className="font-numeros text-2xl font-bold">{formatarTempo(resultado.tempoMs)}</dd>
        </div>
      </dl>

      {resultado.ganhos.length > 0 && (
        <ul className="w-full text-left">
          {resultado.ganhos.map((ganho) => (
            <li key={ganho.motivo} className="flex justify-between border-b-2 border-neutro py-2">
              <span>{MOTIVOS[ganho.motivo]}</span>
              <strong>+{ganho.pontos}</strong>
            </li>
          ))}
        </ul>
      )}

      <p className="flex items-center gap-2 font-extrabold">
        <Icone nome="chama" className="fill-energia text-energia" />
        Sequência de {resultado.diasSeguidos} {resultado.diasSeguidos === 1 ? "dia" : "dias"}
      </p>

      {resultado.trofeusNovos.map((trofeu) => (
        <motion.div
          key={trofeu.codigo}
          initial={{ y: 12, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          className="flex w-full items-center gap-4 rounded-padrao bg-conquista p-4 text-left"
        >
          <IconeDoTrofeu icone={trofeu.icone} tamanho={36} />
          <div>
            <p className="font-extrabold">Troféu novo: {trofeu.nome}</p>
            <p className="text-sm">{trofeu.descricao}</p>
          </div>
        </motion.div>
      ))}

      {resultado.moduloConcluido && (
        <section className="flex w-full flex-col gap-3 rounded-padrao bg-aviso p-4">
          <p className="font-extrabold">Você concluiu o módulo {resultado.moduloConcluido.titulo}!</p>
          {emitir.data ? (
            <a
              href={enderecoDoPdf(emitir.data.codigo)}
              download
              className="flex min-h-11 items-center justify-center gap-2 rounded-padrao bg-primaria text-lg font-extrabold text-white"
            >
              <Icone nome="baixar" />
              Baixar certificado
            </a>
          ) : (
            <Botao
              larguraTotal
              disabled={emitir.isPending}
              onClick={() => resultado.moduloConcluido && emitir.mutate(resultado.moduloConcluido.id)}
            >
              Gerar meu certificado
            </Botao>
          )}
          {emitir.isError && <MensagemDeErro erro={emitir.error} />}
        </section>
      )}

      <Link
        to="/"
        className="flex min-h-14 w-full items-center justify-center rounded-padrao bg-primaria text-[19px] font-extrabold text-white"
      >
        Voltar ao caminho
      </Link>
    </div>
  );
}

import { Link } from "react-router";
import { enderecoDoPdf, useCaminho, useEmitirCertificado, usePerfil } from "../../api/consultas";
import type { LicaoNoCaminho, ModuloNoCaminho } from "../../api/tipos";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Icone } from "../../componentes/ui/Icone";
import { Mascote } from "../../mascote/Mascote";

const DESLOCAMENTOS = ["translate-x-0", "translate-x-16", "translate-x-0", "-translate-x-16"];

export function TelaDoCaminho() {
  const caminho = useCaminho();

  if (caminho.isPending) {
    return <Carregando texto="Montando o seu caminho…" />;
  }
  if (caminho.isError) {
    return <MensagemDeErro erro={caminho.error} aoTentarDeNovo={() => void caminho.refetch()} />;
  }
  if (caminho.data.length === 0) {
    return (
      <div className="flex flex-col items-center gap-4 py-12 text-center">
        <Mascote estado="neutro" />
        <p className="text-lg font-bold">A TI está preparando os primeiros desafios. Volte em breve!</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-10">
      {caminho.data.map((modulo, indice) => (
        <Modulo key={modulo.id} modulo={modulo} numero={indice + 1} />
      ))}
    </div>
  );
}

function Modulo({ modulo, numero }: { modulo: ModuloNoCaminho; numero: number }) {
  return (
    <section aria-labelledby={`modulo-${modulo.id}`} className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 rounded-[20px] bg-primaria p-5 text-white">
        <h2 id={`modulo-${modulo.id}`} className="text-[22px] leading-tight font-extrabold">
          Módulo {numero} · {modulo.titulo}
        </h2>
        <p className="text-lg font-bold">
          {modulo.licoesConcluidas} de {modulo.licoes.length} lições
        </p>
        <div className="flex flex-wrap gap-2">
          {modulo.temMaterial && (
            <Link
              to={`/modulos/${modulo.id}/estudo`}
              className="flex min-h-11 items-center gap-2 rounded-padrao bg-white px-4 font-extrabold text-texto"
            >
              <Icone nome="livro" />
              Estudar o conteúdo
            </Link>
          )}
          {modulo.concluido && <BotaoDeCertificado moduloId={modulo.id} />}
        </div>
      </div>

      <ol className="flex flex-col items-center gap-3">
        {modulo.licoes.map((licao, indice) => (
          <li
            key={licao.id}
            className={`flex flex-col items-center ${DESLOCAMENTOS[indice % DESLOCAMENTOS.length]}`}
          >
            {indice > 0 && (
              <span
                aria-hidden="true"
                className={`mb-3 h-8 w-1.5 rounded-full ${licao.situacao === "BLOQUEADA" ? "bg-neutro" : "bg-primaria"}`}
              />
            )}
            <NoDaLicao licao={licao} />
          </li>
        ))}
      </ol>
    </section>
  );
}

function NoDaLicao({ licao }: { licao: LicaoNoCaminho }) {
  const rotulo = (
    <span className="max-w-40 text-center text-[15px] leading-tight font-bold">{licao.titulo}</span>
  );

  if (licao.situacao === "BLOQUEADA") {
    return (
      <div className="flex flex-col items-center gap-2">
        <span
          role="img"
          aria-label={`Lição bloqueada: ${licao.titulo}`}
          className="flex size-[72px] items-center justify-center rounded-[22px] bg-neutro"
        >
          <Icone nome="cadeado" tamanho={28} />
        </span>
        {rotulo}
      </div>
    );
  }

  const atual = licao.situacao === "DISPONIVEL";
  return (
    <div className="flex flex-col items-center gap-2">
      <Link
        to={`/licoes/${licao.id}`}
        aria-label={atual ? `Começar a lição: ${licao.titulo}` : `Refazer a lição concluída: ${licao.titulo}`}
        className={
          atual
            ? "flex size-24 items-center justify-center rounded-[30px] bg-acerto-suave"
            : "flex size-[72px] items-center justify-center rounded-[22px] bg-primaria text-white"
        }
      >
        {atual ? (
          <span className="flex size-20 items-center justify-center rounded-[24px] bg-primaria text-white">
            <Icone nome="escudo" tamanho={36} />
          </span>
        ) : (
          <Icone nome="certo" tamanho={32} strokeWidth={3} />
        )}
      </Link>
      {rotulo}
      {atual && <span className="rounded-[10px] bg-aviso px-2.5 py-1 text-sm font-bold">Você está aqui</span>}
    </div>
  );
}

function BotaoDeCertificado({ moduloId }: { moduloId: number }) {
  const perfil = usePerfil();
  const emitir = useEmitirCertificado();
  const certificado =
    perfil.data?.certificados.find((existente) => existente.moduloId === moduloId) ?? emitir.data;

  if (certificado) {
    return (
      <a
        href={enderecoDoPdf(certificado.codigo)}
        download
        className="flex min-h-11 items-center gap-2 rounded-padrao bg-conquista px-4 font-extrabold text-texto"
      >
        <Icone nome="baixar" />
        Baixar certificado
      </a>
    );
  }
  return (
    <button
      type="button"
      disabled={emitir.isPending}
      onClick={() => emitir.mutate(moduloId)}
      className="flex min-h-11 items-center gap-2 rounded-padrao bg-conquista px-4 font-extrabold text-texto disabled:opacity-60"
    >
      <Icone nome="certificado" />
      Gerar certificado
    </button>
  );
}

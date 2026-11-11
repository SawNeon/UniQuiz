import { Link, useParams } from "react-router";
import { useCaminho, useMaterial } from "../../api/consultas";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Icone } from "../../componentes/ui/Icone";
import { TextoFormatado } from "../../componentes/ui/TextoFormatado";

export function TelaDeEstudo() {
  const moduloId = Number(useParams().moduloId);
  const material = useMaterial(moduloId);
  const caminho = useCaminho();
  const proximaLicao = caminho.data
    ?.find((modulo) => modulo.id === moduloId)
    ?.licoes.find((licao) => licao.situacao === "DISPONIVEL");

  return (
    <div className="mx-auto flex min-h-dvh max-w-xl flex-col gap-6 px-4 py-4">
      <Link to="/" className="flex min-h-11 items-center gap-2 self-start font-extrabold">
        <Icone nome="voltar" />
        Voltar ao caminho
      </Link>

      {material.isPending && <Carregando />}
      {material.isError && <MensagemDeErro erro={material.error} />}
      {material.isSuccess && (
        <article className="flex flex-col gap-6">
          <p className="text-sm font-extrabold tracking-widest">CONTEÚDO PARA ESTUDAR</p>
          <TextoFormatado conteudo={material.data.conteudo} />
        </article>
      )}

      {proximaLicao && (
        <Link
          to={`/licoes/${proximaLicao.id}`}
          className="mt-auto flex min-h-14 items-center justify-center rounded-padrao bg-primaria text-[19px] font-extrabold text-white"
        >
          Fazer a lição: {proximaLicao.titulo}
        </Link>
      )}
    </div>
  );
}

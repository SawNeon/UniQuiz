import { useParams } from "react-router";
import { useValidacaoDeCertificado } from "../../api/consultas";
import { Carregando } from "../../componentes/ui/Estados";
import { Icone } from "../../componentes/ui/Icone";

export function TelaDeValidacao() {
  const codigo = useParams().codigo ?? "";
  const validacao = useValidacaoDeCertificado(codigo);

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col gap-6 px-4 py-10">
      <h1 className="text-3xl font-extrabold">Validação de certificado</h1>
      {validacao.isPending && <Carregando texto="Conferindo o código…" />}
      {validacao.isError && (
        <section role="alert" className="flex items-start gap-3 rounded-padrao bg-erro-suave p-5">
          <Icone nome="errado" tamanho={28} />
          <p className="font-bold">Não encontramos um certificado com o código {codigo}. Confira se foi digitado certo.</p>
        </section>
      )}
      {validacao.isSuccess && (
        <section className="flex flex-col gap-2 rounded-padrao bg-acerto-suave p-5">
          <p className="flex items-center gap-2 text-xl font-extrabold">
            <Icone nome="certo" strokeWidth={3} />
            Certificado válido
          </p>
          <p>
            <strong>{validacao.data.nome}</strong> concluiu o módulo <strong>{validacao.data.modulo}</strong> do UniQuiz
            em {new Date(`${validacao.data.emitidoEm}T12:00:00`).toLocaleDateString("pt-BR")}.
          </p>
          <p className="text-sm">Código {validacao.data.codigo}</p>
        </section>
      )}
    </main>
  );
}

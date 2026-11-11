import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { type DadosDoModulo, useSalvarModulo } from "../../api/admin";
import type { ModuloParaAdmin } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { TextoFormatado } from "../../componentes/ui/TextoFormatado";
import { AreaDeTexto, Campo, Entrada } from "./componentes";

const esquema = z.object({
  titulo: z.string().trim().min(1, "Dê um título ao módulo.").max(150),
  descricao: z.string().max(500),
  materialEstudo: z.string().max(20_000, "O material passou do limite de 20 mil caracteres."),
});

export function FormularioDeModulo({ modulo, aoTerminar }: { modulo?: ModuloParaAdmin; aoTerminar: () => void }) {
  const salvar = useSalvarModulo();
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<DadosDoModulo>({
    resolver: zodResolver(esquema),
    defaultValues: {
      titulo: modulo?.titulo ?? "",
      descricao: modulo?.descricao ?? "",
      materialEstudo: modulo?.materialEstudo ?? "",
    },
  });
  const material = useWatch({ control, name: "materialEstudo" });

  return (
    <form
      className="flex flex-col gap-4 rounded-padrao border-2 border-neutro p-4"
      onSubmit={handleSubmit((dados) => salvar.mutate({ id: modulo?.id, ...dados }, { onSuccess: aoTerminar }))}
    >
      <Campo rotulo="Título" erro={errors.titulo?.message}>
        <Entrada {...register("titulo")} />
      </Campo>
      <Campo rotulo="Descrição curta" erro={errors.descricao?.message}>
        <Entrada {...register("descricao")} />
      </Campo>
      <div className="grid gap-4 lg:grid-cols-2">
        <Campo
          rotulo="Conteúdo para estudar (aceita # títulos, **negrito** e listas com -)"
          erro={errors.materialEstudo?.message}
        >
          <AreaDeTexto rows={16} className="font-mono text-sm" {...register("materialEstudo")} />
        </Campo>
        <section aria-label="Pré-visualização do conteúdo" className="rounded-padrao bg-neutro/50 p-4">
          <p className="mb-3 text-sm font-extrabold tracking-widest">PRÉ-VISUALIZAÇÃO</p>
          {material ? <TextoFormatado conteudo={material} /> : <p>O conteúdo aparece aqui.</p>}
        </section>
      </div>
      {salvar.isError && <MensagemDeErro erro={salvar.error} />}
      <div className="flex gap-3">
        <Botao type="submit" disabled={salvar.isPending}>
          Salvar módulo
        </Botao>
        <Botao variante="secundario" onClick={aoTerminar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useSalvarTemporada, useTemporadasAdmin } from "../../api/admin";
import type { Temporada } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";

const esquema = z
  .object({
    nome: z.string().trim().min(1, "Dê um nome à temporada.").max(100),
    inicio: z.string().min(1, "Informe o início."),
    fim: z.string().min(1, "Informe o fim."),
    premioTexto: z.string().max(300),
  })
  .refine((dados) => dados.fim >= dados.inicio, {
    path: ["fim"],
    message: "O fim precisa ser depois do início.",
  });

type DadosDaTemporada = z.infer<typeof esquema>;

export function TelaDeTemporadas() {
  const temporadas = useTemporadasAdmin();
  const [editando, setEditando] = useState<Temporada | "nova" | null>(null);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-extrabold">Temporadas</h1>
        {!editando && <Botao onClick={() => setEditando("nova")}>Nova temporada</Botao>}
      </div>
      <p>A classificação vale dentro da temporada. O prêmio aparece no topo da classificação.</p>
      {editando && (
        <FormularioDeTemporada
          key={editando === "nova" ? "nova" : editando.id}
          temporada={editando === "nova" ? undefined : editando}
          aoTerminar={() => setEditando(null)}
        />
      )}
      {temporadas.isPending && <Carregando />}
      {temporadas.isError && <MensagemDeErro erro={temporadas.error} />}
      <ul className="flex flex-col gap-2">
        {temporadas.data?.map((temporada) => (
          <li key={temporada.id} className="flex flex-wrap items-center gap-3 rounded-xl bg-neutro/60 p-3">
            <div className="flex flex-1 flex-col">
              <span className="font-extrabold">{temporada.nome}</span>
              <span className="text-sm">
                {temporada.inicio} a {temporada.fim}
                {temporada.premioTexto ? ` · Prêmio: ${temporada.premioTexto}` : ""}
              </span>
            </div>
            <Botao variante="secundario" onClick={() => setEditando(temporada)}>
              Editar
            </Botao>
          </li>
        ))}
      </ul>
    </div>
  );
}

function FormularioDeTemporada({ temporada, aoTerminar }: { temporada?: Temporada; aoTerminar: () => void }) {
  const salvar = useSalvarTemporada();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<DadosDaTemporada>({
    resolver: zodResolver(esquema),
    defaultValues: {
      nome: temporada?.nome ?? "",
      inicio: temporada?.inicio ?? "",
      fim: temporada?.fim ?? "",
      premioTexto: temporada?.premioTexto ?? "",
    },
  });

  return (
    <form
      className="grid gap-4 rounded-padrao border-2 border-neutro p-4 sm:grid-cols-2"
      onSubmit={handleSubmit((dados) =>
        salvar.mutate({ id: temporada?.id, ...dados }, { onSuccess: aoTerminar }),
      )}
    >
      <Campo rotulo="Nome" erro={errors.nome?.message}>
        <Entrada placeholder="Dezembro de 2026" {...register("nome")} />
      </Campo>
      <Campo rotulo="Prêmio do 1º lugar" erro={errors.premioTexto?.message}>
        <Entrada placeholder="Brinde e destaque no mural" {...register("premioTexto")} />
      </Campo>
      <Campo rotulo="Início" erro={errors.inicio?.message}>
        <Entrada type="date" {...register("inicio")} />
      </Campo>
      <Campo rotulo="Fim" erro={errors.fim?.message}>
        <Entrada type="date" {...register("fim")} />
      </Campo>
      {salvar.isError && (
        <div className="sm:col-span-2">
          <MensagemDeErro erro={salvar.error} />
        </div>
      )}
      <div className="flex gap-3 sm:col-span-2">
        <Botao type="submit" disabled={salvar.isPending}>
          Salvar temporada
        </Botao>
        <Botao variante="secundario" onClick={aoTerminar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}

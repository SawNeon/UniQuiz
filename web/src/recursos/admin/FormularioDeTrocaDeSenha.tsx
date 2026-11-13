import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useTrocarMinhaSenha } from "../../api/admin";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";
import { forcaDaSenha, TAMANHO_MAXIMO_DA_SENHA, TAMANHO_MINIMO_DA_SENHA } from "./forcaDaSenha";
import { useContaDoPainel } from "./painel";

const CORES_DA_FORCA = ["bg-erro-suave", "bg-erro-suave", "bg-aviso", "bg-acerto-suave", "bg-primaria"];

const esquema = z
  .object({
    senhaAtual: z.string().min(1, "Informe a senha atual."),
    novaSenha: z
      .string()
      .min(TAMANHO_MINIMO_DA_SENHA, `Use pelo menos ${TAMANHO_MINIMO_DA_SENHA} caracteres.`)
      .max(TAMANHO_MAXIMO_DA_SENHA, `Use no máximo ${TAMANHO_MAXIMO_DA_SENHA} caracteres.`),
    confirmacao: z.string(),
  })
  .refine((dados) => dados.confirmacao === dados.novaSenha, {
    path: ["confirmacao"],
    message: "As duas senhas precisam ser iguais.",
  })
  .refine((dados) => dados.novaSenha !== dados.senhaAtual, {
    path: ["novaSenha"],
    message: "A nova senha precisa ser diferente da atual.",
  });

type DadosDaTroca = z.infer<typeof esquema>;

export function FormularioDeTrocaDeSenha({
  rotuloDaSenhaAtual = "Senha atual",
  aoTrocar,
}: {
  rotuloDaSenhaAtual?: string;
  aoTrocar?: () => void;
}) {
  const conta = useContaDoPainel();
  const trocar = useTrocarMinhaSenha();
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<DadosDaTroca>({ resolver: zodResolver(esquema) });
  const novaSenha = watch("novaSenha") ?? "";

  return (
    <form
      className="flex max-w-md flex-col gap-4"
      onSubmit={handleSubmit(({ senhaAtual, novaSenha }) =>
        trocar.mutate(
          { senhaAtual, novaSenha },
          {
            onSuccess: () => {
              reset();
              aoTrocar?.();
            },
          },
        ),
      )}
    >
      <Campo rotulo={rotuloDaSenhaAtual} erro={errors.senhaAtual?.message}>
        <Entrada type="password" autoComplete="current-password" {...register("senhaAtual")} />
      </Campo>
      <Campo rotulo="Nova senha" erro={errors.novaSenha?.message}>
        <Entrada type="password" autoComplete="new-password" {...register("novaSenha")} />
      </Campo>
      {novaSenha.length > 0 && <MedidorDeForca senha={novaSenha} usuario={conta.usuario} />}
      <Campo rotulo="Repita a nova senha" erro={errors.confirmacao?.message}>
        <Entrada type="password" autoComplete="new-password" {...register("confirmacao")} />
      </Campo>
      <p className="text-sm">
        Dica: uma frase fácil de lembrar e difícil de adivinhar, como “café forte antes da reunião”, é mais
        segura do que uma palavra curta cheia de símbolos. Não use o seu login nem palavras como “senha” ou
        “unimed”.
      </p>
      {trocar.isError && <MensagemDeErro erro={trocar.error} />}
      {trocar.isSuccess && (
        <p role="status" className="rounded-padrao bg-acerto-suave p-3 font-bold">
          Senha trocada. As suas outras sessões foram encerradas.
        </p>
      )}
      <Botao type="submit" disabled={trocar.isPending}>
        Trocar senha
      </Botao>
    </form>
  );
}

function MedidorDeForca({ senha, usuario }: { senha: string; usuario: string }) {
  const forca = forcaDaSenha(senha, usuario);
  return (
    <div className="flex flex-col gap-1">
      <div className="grid grid-cols-4 gap-1" aria-hidden="true">
        {[1, 2, 3, 4].map((degrau) => (
          <span
            key={degrau}
            className={`h-2 rounded-full ${degrau <= Math.max(forca.nivel, 1) ? CORES_DA_FORCA[forca.nivel] : "bg-neutro"}`}
          />
        ))}
      </div>
      <p aria-live="polite" className="text-sm font-bold">
        Força da senha: {forca.rotulo}
      </p>
    </div>
  );
}

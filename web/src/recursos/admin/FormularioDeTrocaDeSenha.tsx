import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useTrocarMinhaSenha } from "../../api/admin";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";

const TAMANHO_MINIMO = 12;
const TAMANHO_MAXIMO = 128;

const esquema = z
  .object({
    senhaAtual: z.string().min(1, "Informe a senha atual."),
    novaSenha: z
      .string()
      .min(TAMANHO_MINIMO, `Use pelo menos ${TAMANHO_MINIMO} caracteres.`)
      .max(TAMANHO_MAXIMO, `Use no máximo ${TAMANHO_MAXIMO} caracteres.`),
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
  const trocar = useTrocarMinhaSenha();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosDaTroca>({ resolver: zodResolver(esquema) });

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

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { z } from "zod";
import { useLoginAdmin } from "../../api/admin";
import { Botao } from "../../componentes/ui/Botao";
import { MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";

const esquema = z.object({
  usuario: z.string().trim().min(1, "Informe o usuário."),
  senha: z.string().min(1, "Informe a senha."),
});

type Credenciais = z.infer<typeof esquema>;

export function LoginAdmin() {
  const navegar = useNavigate();
  const login = useLoginAdmin();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Credenciais>({ resolver: zodResolver(esquema) });

  return (
    <main className="mx-auto flex min-h-dvh max-w-sm flex-col justify-center gap-6 px-4">
      <h1 className="text-3xl font-extrabold">UniQuiz · Administração</h1>
      <form
        className="flex flex-col gap-4"
        onSubmit={handleSubmit((credenciais) =>
          login.mutate(credenciais, { onSuccess: () => void navegar("/admin", { replace: true }) }),
        )}
      >
        <Campo rotulo="Usuário" erro={errors.usuario?.message}>
          <Entrada autoComplete="username" {...register("usuario")} />
        </Campo>
        <Campo rotulo="Senha" erro={errors.senha?.message}>
          <Entrada type="password" autoComplete="current-password" {...register("senha")} />
        </Campo>
        {login.isError && <MensagemDeErro erro={login.error} />}
        <Botao type="submit" larguraTotal disabled={login.isPending}>
          Entrar
        </Botao>
      </form>
    </main>
  );
}

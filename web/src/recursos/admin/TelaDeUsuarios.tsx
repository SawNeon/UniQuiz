import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import {
  type AcaoSobreUsuario,
  useAcaoSobreUsuario,
  useCriarUsuarioDoPainel,
  useEditarUsuarioDoPainel,
  useRedefinirSenhaDoUsuario,
  useSetoresDoPainel,
  useUsuariosDoPainel,
} from "../../api/admin";
import type { ItemDaLista, PerfilDoPainel, SenhaProvisoria, UsuarioDoPainel } from "../../api/tipos";
import { Botao } from "../../componentes/ui/Botao";
import { Carregando, MensagemDeErro } from "../../componentes/ui/Estados";
import { Campo, Entrada } from "./componentes";
import { formatarMomento, PERFIS_DO_PAINEL, useContaDoPainel } from "./painel";

const PERFIS = Object.keys(PERFIS_DO_PAINEL) as PerfilDoPainel[];

const esquema = z
  .object({
    nome: z.string().trim().min(1, "Informe o nome.").max(150),
    usuario: z
      .string()
      .trim()
      .toLowerCase()
      .regex(/^[a-z0-9._-]{3,60}$/, "Use de 3 a 60 letras sem acento, números, ponto, hífen ou sublinhado."),
    perfil: z.enum(["ADMINISTRADOR", "GESTOR", "AUDITOR"]),
    setores: z.array(z.string()),
  })
  .refine((dados) => dados.perfil !== "GESTOR" || dados.setores.length > 0, {
    path: ["setores"],
    message: "Escolha ao menos um setor para o Gestor.",
  });

type DadosDoFormulario = z.input<typeof esquema>;

export function TelaDeUsuarios() {
  const usuarios = useUsuariosDoPainel();
  const setores = useSetoresDoPainel();
  const [editando, setEditando] = useState<UsuarioDoPainel | "novo" | null>(null);
  const [senhaGerada, setSenhaGerada] = useState<SenhaProvisoria | null>(null);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-extrabold">Usuários do painel</h1>
        {!editando && <Botao onClick={() => setEditando("novo")}>Novo usuário</Botao>}
      </div>
      <p className="max-w-prose">
        Cada pessoa tem o próprio login. Quem é criado aqui recebe uma senha provisória e precisa trocá-la no
        primeiro acesso.
      </p>
      {editando && (
        <FormularioDeUsuario
          key={editando === "novo" ? "novo" : editando.id}
          usuario={editando === "novo" ? undefined : editando}
          setores={setores.data ?? []}
          aoCriar={setSenhaGerada}
          aoTerminar={() => setEditando(null)}
        />
      )}
      {usuarios.isPending && <Carregando />}
      {usuarios.isError && <MensagemDeErro erro={usuarios.error} />}
      <ul className="flex flex-col gap-2">
        {usuarios.data?.map((usuario) => (
          <LinhaDoUsuario
            key={usuario.id}
            usuario={usuario}
            setores={setores.data ?? []}
            aoEditar={() => setEditando(usuario)}
            aoRedefinirSenha={setSenhaGerada}
          />
        ))}
      </ul>
      {senhaGerada && <JanelaDaSenhaProvisoria senha={senhaGerada} aoFechar={() => setSenhaGerada(null)} />}
    </div>
  );
}

function LinhaDoUsuario({
  usuario,
  setores,
  aoEditar,
  aoRedefinirSenha,
}: {
  usuario: UsuarioDoPainel;
  setores: ItemDaLista[];
  aoEditar: () => void;
  aoRedefinirSenha: (senha: SenhaProvisoria) => void;
}) {
  const conta = useContaDoPainel();
  const acao = useAcaoSobreUsuario();
  const redefinir = useRedefinirSenhaDoUsuario();
  const souEu = usuario.id === conta.id;
  const nomesDosSetores = setores
    .filter((setor) => usuario.setores.includes(setor.id))
    .map((setor) => setor.nome);

  function executar(tipo: AcaoSobreUsuario, pergunta?: string) {
    if (pergunta && !window.confirm(pergunta)) {
      return;
    }
    acao.mutate({ id: usuario.id, acao: tipo });
  }

  return (
    <li
      aria-label={usuario.nome}
      className={`flex flex-col gap-3 rounded-xl p-3 ${usuario.ativo ? "bg-neutro/60" : "bg-neutro/30"}`}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col">
          <span className="font-extrabold">
            {usuario.nome}
            {souEu && " (você)"}
          </span>
          <span className="text-sm">
            {usuario.usuario} · {PERFIS_DO_PAINEL[usuario.perfil].rotulo}
            {nomesDosSetores.length > 0 && ` · ${nomesDosSetores.join(", ")}`}
          </span>
          <span className="text-sm">
            {usuario.ultimoAcessoEm
              ? `Último acesso em ${formatarMomento(usuario.ultimoAcessoEm)}`
              : "Ainda não entrou"}
          </span>
        </div>
        <span className="flex flex-wrap gap-1">
          {!usuario.ativo && <Selo cor="bg-neutro">Desativado</Selo>}
          {usuario.bloqueado && <Selo cor="bg-erro-suave">Bloqueado</Selo>}
          {usuario.precisaTrocarSenha && <Selo cor="bg-aviso">Senha provisória</Selo>}
        </span>
      </div>
      <div className="flex flex-wrap gap-2">
        <Botao variante="secundario" onClick={aoEditar}>
          Editar
        </Botao>
        {usuario.bloqueado && (
          <Botao variante="secundario" onClick={() => executar("desbloquear")}>
            Desbloquear
          </Botao>
        )}
        {!souEu && (
          <>
            <Botao
              variante="secundario"
              disabled={redefinir.isPending}
              onClick={() => {
                if (
                  window.confirm(
                    `Gerar uma nova senha provisória para ${usuario.nome}? A atual deixa de valer.`,
                  )
                ) {
                  redefinir.mutate(usuario.id, { onSuccess: aoRedefinirSenha });
                }
              }}
            >
              Redefinir senha
            </Botao>
            <Botao
              variante="secundario"
              onClick={() =>
                executar("encerrar-sessoes", `Desconectar ${usuario.nome} de todos os aparelhos?`)
              }
            >
              Encerrar sessões
            </Botao>
            {usuario.ativo ? (
              <Botao
                variante="secundario"
                onClick={() =>
                  executar("desativar", `Desativar ${usuario.nome}? A pessoa sai do painel na hora.`)
                }
              >
                Desativar
              </Botao>
            ) : (
              <Botao variante="secundario" onClick={() => executar("reativar")}>
                Reativar
              </Botao>
            )}
          </>
        )}
      </div>
      {(acao.error ?? redefinir.error) && <MensagemDeErro erro={acao.error ?? redefinir.error} />}
    </li>
  );
}

function Selo({ cor, children }: { cor: string; children: string }) {
  return <span className={`rounded-lg px-2 py-0.5 text-sm font-bold ${cor}`}>{children}</span>;
}

function FormularioDeUsuario({
  usuario,
  setores,
  aoCriar,
  aoTerminar,
}: {
  usuario?: UsuarioDoPainel;
  setores: ItemDaLista[];
  aoCriar: (senha: SenhaProvisoria) => void;
  aoTerminar: () => void;
}) {
  const criar = useCriarUsuarioDoPainel();
  const editar = useEditarUsuarioDoPainel();
  const salvando = criar.isPending || editar.isPending;
  const erro = criar.error ?? editar.error;
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<DadosDoFormulario>({
    resolver: zodResolver(esquema),
    defaultValues: {
      nome: usuario?.nome ?? "",
      usuario: usuario?.usuario ?? "",
      perfil: usuario?.perfil ?? "GESTOR",
      setores: usuario?.setores.map(String) ?? [],
    },
  });
  const perfil = watch("perfil");

  return (
    <form
      aria-label={usuario ? `Editar ${usuario.nome}` : "Novo usuário"}
      className="grid gap-4 rounded-padrao border-2 border-neutro p-4 sm:grid-cols-2"
      onSubmit={handleSubmit((dados) => {
        const comum = {
          nome: dados.nome,
          perfil: dados.perfil,
          setores: dados.perfil === "GESTOR" ? dados.setores.map(Number) : [],
        };
        if (usuario) {
          editar.mutate({ id: usuario.id, ...comum }, { onSuccess: aoTerminar });
        } else {
          criar.mutate(
            { ...comum, usuario: dados.usuario.trim().toLowerCase() },
            {
              onSuccess: (senha) => {
                aoCriar(senha);
                aoTerminar();
              },
            },
          );
        }
      })}
    >
      <Campo rotulo="Nome" erro={errors.nome?.message}>
        <Entrada autoComplete="off" {...register("nome")} />
      </Campo>
      <Campo rotulo="Login" erro={errors.usuario?.message}>
        <Entrada
          autoComplete="off"
          autoCapitalize="none"
          readOnly={usuario !== undefined}
          placeholder="maria.silva"
          {...register("usuario")}
        />
      </Campo>
      <fieldset className="flex flex-col gap-2 sm:col-span-2">
        <legend className="font-extrabold">Perfil</legend>
        {PERFIS.map((opcao) => (
          <label key={opcao} className="flex min-h-11 items-start gap-3 rounded-xl bg-neutro/40 p-3">
            <input type="radio" value={opcao} className="mt-1 size-5" {...register("perfil")} />
            <span>
              <span className="font-extrabold">{PERFIS_DO_PAINEL[opcao].rotulo}</span>
              <span className="block text-sm">{PERFIS_DO_PAINEL[opcao].descricao}</span>
            </span>
          </label>
        ))}
      </fieldset>
      {perfil === "GESTOR" && (
        <fieldset className="flex flex-col gap-2 sm:col-span-2">
          <legend className="font-extrabold">Setores que o Gestor acompanha</legend>
          <div className="grid max-h-64 gap-1 overflow-y-auto sm:grid-cols-2">
            {setores.map((setor) => (
              <label key={setor.id} className="flex min-h-11 items-center gap-3 rounded-xl px-2">
                <input type="checkbox" value={String(setor.id)} className="size-5" {...register("setores")} />
                {setor.nome}
              </label>
            ))}
          </div>
          {errors.setores?.message && (
            <span role="alert" className="text-sm font-bold">
              {errors.setores.message}
            </span>
          )}
        </fieldset>
      )}
      {erro && (
        <div className="sm:col-span-2">
          <MensagemDeErro erro={erro} />
        </div>
      )}
      <div className="flex gap-3 sm:col-span-2">
        <Botao type="submit" disabled={salvando}>
          {usuario ? "Salvar alterações" : "Criar usuário"}
        </Botao>
        <Botao variante="secundario" onClick={aoTerminar}>
          Cancelar
        </Botao>
      </div>
    </form>
  );
}

function JanelaDaSenhaProvisoria({ senha, aoFechar }: { senha: SenhaProvisoria; aoFechar: () => void }) {
  const janela = useRef<HTMLDialogElement>(null);
  const [copiada, setCopiada] = useState(false);

  useEffect(() => {
    const elemento = janela.current;
    if (!elemento || elemento.open) {
      return;
    }
    if (typeof elemento.showModal === "function") {
      elemento.showModal();
    } else {
      elemento.setAttribute("open", "");
    }
  }, []);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(senha.senha);
      setCopiada(true);
    } catch {
      setCopiada(false);
    }
  }

  return (
    <dialog
      ref={janela}
      aria-labelledby="titulo-senha-provisoria"
      onClose={aoFechar}
      className="m-auto max-w-md rounded-padrao p-6 backdrop:bg-black/50"
    >
      <div className="flex flex-col gap-4">
        <h2 id="titulo-senha-provisoria" className="text-2xl font-extrabold">
          Senha provisória de {senha.usuario}
        </h2>
        <p>
          Entregue esta senha pessoalmente ou por um canal seguro. <strong>Ela não aparece de novo.</strong>{" "}
          No primeiro acesso, a pessoa vai criar a própria senha.
        </p>
        <output
          aria-label="Senha provisória"
          className="rounded-xl bg-neutro p-3 text-center font-mono text-xl font-bold tracking-wider select-all"
        >
          {senha.senha}
        </output>
        <div className="flex flex-wrap gap-3">
          <Botao onClick={() => void copiar()}>{copiada ? "Copiada!" : "Copiar senha"}</Botao>
          <Botao variante="secundario" onClick={aoFechar}>
            Já anotei, fechar
          </Botao>
        </div>
      </div>
    </dialog>
  );
}

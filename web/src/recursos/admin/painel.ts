import { createContext, useContext } from "react";
import type { ContaLogada, PerfilDoPainel, Permissao } from "../../api/tipos";

export interface SecaoDoPainel {
  para: string;
  rotulo: string;
  permissao?: Permissao;
}

export const SECOES_DO_PAINEL: SecaoDoPainel[] = [
  { para: "/admin", rotulo: "Conteúdo", permissao: "GERIR_CONTEUDO" },
  { para: "/admin/colaboradores", rotulo: "Colaboradores", permissao: "GERIR_COLABORADORES" },
  { para: "/admin/temporadas", rotulo: "Temporadas", permissao: "GERIR_TEMPORADAS" },
  { para: "/admin/usuarios", rotulo: "Usuários do painel", permissao: "GERIR_USUARIOS_DO_PAINEL" },
  { para: "/admin/minha-conta", rotulo: "Minha conta" },
];

export const PERFIS_DO_PAINEL: Record<PerfilDoPainel, { rotulo: string; descricao: string }> = {
  ADMINISTRADOR: {
    rotulo: "Administrador",
    descricao: "Faz tudo no painel, inclusive criar e desativar outros usuários.",
  },
  GESTOR: {
    rotulo: "Gestor",
    descricao: "Vê e exporta os relatórios apenas dos setores escolhidos. Não altera nada.",
  },
  AUDITOR: {
    rotulo: "Auditor",
    descricao: "Vê e exporta todos os relatórios e o registro de auditoria. Não altera nada.",
  },
};

export const ContextoDaConta = createContext<ContaLogada | null>(null);

export function useContaDoPainel(): ContaLogada {
  const conta = useContext(ContextoDaConta);
  if (!conta) {
    throw new Error("useContaDoPainel precisa estar dentro do LayoutAdmin.");
  }
  return conta;
}

export function pode(conta: ContaLogada, permissao?: Permissao): boolean {
  return permissao === undefined || conta.permissoes.includes(permissao);
}

export function secoesPermitidas(conta: ContaLogada): SecaoDoPainel[] {
  return SECOES_DO_PAINEL.filter((secao) => pode(conta, secao.permissao));
}

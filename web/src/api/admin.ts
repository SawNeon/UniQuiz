import { type QueryKey, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./cliente";
import type {
  ColaboradorParaAdmin,
  ContaLogada,
  DadosDoDesafio,
  DadosDoUsuarioDoPainel,
  DesafioParaAdmin,
  ItemDaLista,
  ModuloParaAdmin,
  PorNivel,
  ResumoDaImportacao,
  SenhaProvisoria,
  StatusPublicacao,
  Temporada,
  UsuarioDoPainel,
} from "./tipos";

export const chavesAdmin = {
  eu: ["admin", "eu"] as const,
  modulos: ["admin", "modulos"] as const,
  desafios: (licaoId: number) => ["admin", "desafios", licaoId] as const,
  colaboradores: (busca: string) => ["admin", "colaboradores", busca] as const,
  temporadas: ["admin", "temporadas"] as const,
  usuarios: ["admin", "usuarios"] as const,
  setoresDoPainel: ["admin", "usuarios", "setores"] as const,
};

function useAcao<TEntrada, TSaida = void>(
  acao: (entrada: TEntrada) => Promise<TSaida>,
  invalidar: QueryKey[],
) {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: acao,
    onSuccess: () => Promise.all(invalidar.map((chave) => cliente.invalidateQueries({ queryKey: chave }))),
  });
}

export function useAdminEu() {
  return useQuery({
    queryKey: chavesAdmin.eu,
    queryFn: () => api.get<ContaLogada>("/admin/eu"),
    retry: false,
  });
}

export function useTrocarMinhaSenha() {
  return useAcao(
    (dados: { senhaAtual: string; novaSenha: string }) => api.post("/admin/minha-conta/senha", dados),
    [chavesAdmin.eu],
  );
}

export function useUsuariosDoPainel() {
  return useQuery({
    queryKey: chavesAdmin.usuarios,
    queryFn: () => api.get<UsuarioDoPainel[]>("/admin/usuarios"),
  });
}

export function useSetoresDoPainel() {
  return useQuery({
    queryKey: chavesAdmin.setoresDoPainel,
    queryFn: () => api.get<ItemDaLista[]>("/admin/usuarios/setores"),
  });
}

export function useCriarUsuarioDoPainel() {
  return useAcao(
    (dados: DadosDoUsuarioDoPainel & { usuario: string }) =>
      api.post<SenhaProvisoria>("/admin/usuarios", dados),
    [chavesAdmin.usuarios],
  );
}

export function useEditarUsuarioDoPainel() {
  return useAcao(
    ({ id, ...dados }: DadosDoUsuarioDoPainel & { id: number }) => api.put(`/admin/usuarios/${id}`, dados),
    [chavesAdmin.usuarios, chavesAdmin.eu],
  );
}

export type AcaoSobreUsuario = "desativar" | "reativar" | "desbloquear" | "encerrar-sessoes";

export function useAcaoSobreUsuario() {
  return useAcao(
    ({ id, acao }: { id: number; acao: AcaoSobreUsuario }) => api.post(`/admin/usuarios/${id}/${acao}`),
    [chavesAdmin.usuarios],
  );
}

export function useRedefinirSenhaDoUsuario() {
  return useAcao(
    (id: number) => api.post<SenhaProvisoria>(`/admin/usuarios/${id}/redefinir-senha`),
    [chavesAdmin.usuarios],
  );
}

export function useLoginAdmin() {
  return useAcao(
    (dados: { usuario: string; senha: string }) => api.post("/admin/login", dados),
    [chavesAdmin.eu],
  );
}

export function useSairAdmin() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: () => api.delete("/admin/sessao"),
    onSuccess: () => cliente.removeQueries({ queryKey: ["admin"] }),
  });
}

export function useModulosAdmin() {
  return useQuery({
    queryKey: chavesAdmin.modulos,
    queryFn: () => api.get<ModuloParaAdmin[]>("/admin/modulos"),
  });
}

export interface DadosDoModulo {
  titulo: string;
  descricao: string;
  materialEstudo: string;
}

export function useSalvarModulo() {
  return useAcao(
    ({ id, ...dados }: DadosDoModulo & { id?: number }) =>
      id
        ? api.put<ModuloParaAdmin>(`/admin/modulos/${id}`, dados)
        : api.post<ModuloParaAdmin>("/admin/modulos", dados),
    [chavesAdmin.modulos],
  );
}

export function useStatus(recurso: "modulos" | "licoes" | "desafios", licaoId?: number) {
  return useAcao(
    ({ id, status }: { id: number; status: StatusPublicacao }) =>
      api.post(`/admin/${recurso}/${id}/status`, { status }),
    licaoId ? [chavesAdmin.modulos, chavesAdmin.desafios(licaoId)] : [chavesAdmin.modulos],
  );
}

export function useOrdem(caminho: string, invalidar: QueryKey[]) {
  return useAcao((ids: number[]) => api.patch(caminho, { ids }), invalidar);
}

export function useCriarLicao() {
  return useAcao(
    ({ moduloId, titulo, quantidades }: { moduloId: number; titulo: string; quantidades: PorNivel }) =>
      api.post(`/admin/modulos/${moduloId}/licoes`, { titulo, quantidades }),
    [chavesAdmin.modulos],
  );
}

export function useRenomearLicao() {
  return useAcao(
    ({ licaoId, titulo }: { licaoId: number; titulo: string }) =>
      api.put(`/admin/licoes/${licaoId}`, { titulo }),
    [chavesAdmin.modulos],
  );
}

export function useDefinirQuantidades() {
  return useAcao(
    ({ licaoId, titulo, quantidades }: { licaoId: number; titulo: string; quantidades: PorNivel }) =>
      api.put(`/admin/licoes/${licaoId}`, { titulo, quantidades }),
    [chavesAdmin.modulos],
  );
}

export function useDesafiosAdmin(licaoId: number) {
  return useQuery({
    queryKey: chavesAdmin.desafios(licaoId),
    queryFn: () => api.get<DesafioParaAdmin[]>(`/admin/licoes/${licaoId}/desafios`),
  });
}

export function useSalvarDesafio(licaoId: number) {
  return useAcao(
    ({ id, dados }: { id?: number; dados: DadosDoDesafio }) =>
      id
        ? api.put<DesafioParaAdmin>(`/admin/desafios/${id}`, dados)
        : api.post<DesafioParaAdmin>(`/admin/licoes/${licaoId}/desafios`, dados),
    [chavesAdmin.desafios(licaoId), chavesAdmin.modulos],
  );
}

export function useDuplicarDesafio(licaoId: number) {
  return useAcao((id: number) => api.post(`/admin/desafios/${id}/duplicar`), [chavesAdmin.desafios(licaoId)]);
}

export function useColaboradoresAdmin(busca: string) {
  return useQuery({
    queryKey: chavesAdmin.colaboradores(busca),
    queryFn: () => api.get<ColaboradorParaAdmin[]>(`/admin/colaboradores?busca=${encodeURIComponent(busca)}`),
  });
}

export function useImportacao(etapa: "previa" | "confirmar") {
  return useAcao(
    (arquivo: File) => {
      const formulario = new FormData();
      formulario.append("arquivo", arquivo);
      return api.post<ResumoDaImportacao>(`/admin/colaboradores/importacao/${etapa}`, formulario);
    },
    etapa === "confirmar" ? [["admin", "colaboradores"]] : [],
  );
}

export function useAjustarColaborador() {
  return useAcao(
    ({ id, nome, ativo }: { id: number; nome: string; ativo: boolean }) =>
      api.patch(`/admin/colaboradores/${id}`, { nome, ativo }),
    [["admin", "colaboradores"]],
  );
}

export function useCadastrarColaborador() {
  return useAcao(
    (dados: { nome: string; empresa: string; setor: string; pin?: string }) =>
      api.post<ColaboradorParaAdmin>("/admin/colaboradores", dados),
    [["admin", "colaboradores"], ["empresas"], ["setores"], ["colaboradores"]],
  );
}

export function useRedefinirPin() {
  return useAcao(
    (id: number) => api.post(`/admin/colaboradores/${id}/redefinir-pin`),
    [["admin", "colaboradores"]],
  );
}

export function useZerarPontos() {
  return useAcao((id: number) => api.post(`/admin/colaboradores/${id}/zerar-pontos`), []);
}

export function useTemporadasAdmin() {
  return useQuery({
    queryKey: chavesAdmin.temporadas,
    queryFn: () => api.get<Temporada[]>("/admin/temporadas"),
  });
}

export function useSalvarTemporada() {
  return useAcao(
    ({ id, ...dados }: Omit<Temporada, "id"> & { id?: number }) =>
      id
        ? api.put<Temporada>(`/admin/temporadas/${id}`, dados)
        : api.post<Temporada>("/admin/temporadas", dados),
    [chavesAdmin.temporadas],
  );
}

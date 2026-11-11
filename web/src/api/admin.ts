import { type QueryKey, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./cliente";
import type {
  ColaboradorParaAdmin,
  DadosDoDesafio,
  DesafioParaAdmin,
  ModuloParaAdmin,
  ResumoDaImportacao,
  StatusPublicacao,
  Temporada,
} from "./tipos";

export const chavesAdmin = {
  eu: ["admin", "eu"] as const,
  modulos: ["admin", "modulos"] as const,
  desafios: (licaoId: number) => ["admin", "desafios", licaoId] as const,
  colaboradores: (busca: string) => ["admin", "colaboradores", busca] as const,
  temporadas: ["admin", "temporadas"] as const,
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
    queryFn: () => api.get<{ id: number }>("/admin/eu"),
    retry: false,
  });
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
    ({ moduloId, titulo }: { moduloId: number; titulo: string }) =>
      api.post(`/admin/modulos/${moduloId}/licoes`, { titulo }),
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

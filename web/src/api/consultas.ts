import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./cliente";
import type {
  Certificado,
  Classificacao,
  ColaboradorDaEntrada,
  ItemDaLista,
  Material,
  ModuloNoCaminho,
  Perfil,
  ValidacaoDeCertificado,
} from "./tipos";

export const chaves = {
  eu: ["eu"] as const,
  caminho: ["caminho"] as const,
  certificados: ["certificados"] as const,
  classificacao: ["classificacao"] as const,
  rapidos: (licaoId: number) => ["classificacao", "rapidos", licaoId] as const,
  material: (moduloId: number) => ["material", moduloId] as const,
};

export function usePerfil() {
  return useQuery({ queryKey: chaves.eu, queryFn: () => api.get<Perfil>("/eu"), retry: false });
}

export function useCaminho() {
  return useQuery({ queryKey: chaves.caminho, queryFn: () => api.get<ModuloNoCaminho[]>("/caminho") });
}

export function useMaterial(moduloId: number) {
  return useQuery({
    queryKey: chaves.material(moduloId),
    queryFn: () => api.get<Material>(`/modulos/${moduloId}/estudo`),
  });
}

export function useClassificacao() {
  return useQuery({
    queryKey: chaves.classificacao,
    queryFn: () => api.get<Classificacao>("/classificacao"),
  });
}

export function useMaisRapidos(licaoId: number | null) {
  return useQuery({
    queryKey: chaves.rapidos(licaoId ?? 0),
    queryFn: () => api.get<Classificacao>(`/classificacao/rapidos?licaoId=${licaoId}`),
    enabled: licaoId !== null,
  });
}

export function useEmpresas() {
  return useQuery({ queryKey: ["empresas"], queryFn: () => api.get<ItemDaLista[]>("/empresas") });
}

export function useSetores(empresaId: number | null) {
  return useQuery({
    queryKey: ["setores", empresaId],
    queryFn: () => api.get<ItemDaLista[]>(`/empresas/${empresaId}/setores`),
    enabled: empresaId !== null,
  });
}

export function useColaboradoresDoSetor(setorId: number | null) {
  return useQuery({
    queryKey: ["colaboradores", setorId],
    queryFn: () => api.get<ColaboradorDaEntrada[]>(`/setores/${setorId}/colaboradores`),
    enabled: setorId !== null,
  });
}

export function useEntrar() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: (dados: { colaboradorId: number; pin: string }) => api.post("/sessao", dados),
    onSuccess: () => cliente.invalidateQueries(),
  });
}

export function useSair() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: () => api.delete("/sessao"),
    onSuccess: () => cliente.clear(),
  });
}

export function useEmitirCertificado() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: (moduloId: number) => api.post<Certificado>(`/modulos/${moduloId}/certificado`),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: chaves.eu });
    },
  });
}

export function useValidacaoDeCertificado(codigo: string) {
  return useQuery({
    queryKey: ["validacao", codigo],
    queryFn: () => api.get<ValidacaoDeCertificado>(`/publico/certificados/${encodeURIComponent(codigo)}`),
    retry: false,
  });
}

export function enderecoDoPdf(codigo: string): string {
  return `/api/certificados/${encodeURIComponent(codigo)}/pdf`;
}

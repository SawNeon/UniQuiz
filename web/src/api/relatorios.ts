import { useQuery } from "@tanstack/react-query";
import { api } from "./cliente";
import type {
  Conformidade,
  Desempenho,
  Engajamento,
  FiltroDeDesempenho,
  FiltroDePessoas,
  FiltrosDosRelatorios,
  FormatoDeExportacao,
  PaginaDePessoas,
  RelatorioExportavel,
} from "./tipos";

type Valor = string | number | undefined | null;

export function parametros(valores: Record<string, Valor>): string {
  const busca = new URLSearchParams();
  Object.entries(valores).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== "") {
      busca.set(chave, String(valor));
    }
  });
  return busca.toString();
}

function comParametros(caminho: string, valores: Record<string, Valor>): string {
  const consulta = parametros(valores);
  return consulta ? `${caminho}?${consulta}` : caminho;
}

const chaves = {
  filtros: ["admin", "relatorios", "filtros"] as const,
  conformidade: (setorId?: number) => ["admin", "relatorios", "conformidade", setorId ?? null] as const,
  pessoas: (filtro: FiltroDePessoas) => ["admin", "relatorios", "pessoas", filtro] as const,
  desempenho: (filtro: FiltroDeDesempenho) => ["admin", "relatorios", "desempenho", filtro] as const,
  engajamento: (semanas: number) => ["admin", "relatorios", "engajamento", semanas] as const,
};

export function useFiltrosDosRelatorios() {
  return useQuery({
    queryKey: chaves.filtros,
    queryFn: () => api.get<FiltrosDosRelatorios>("/admin/relatorios/filtros"),
  });
}

export function useConformidade(setorId?: number) {
  return useQuery({
    queryKey: chaves.conformidade(setorId),
    queryFn: () => api.get<Conformidade>(comParametros("/admin/relatorios/conformidade", { setorId })),
  });
}

export function usePessoasDaConformidade(filtro: FiltroDePessoas) {
  return useQuery({
    queryKey: chaves.pessoas(filtro),
    queryFn: () =>
      api.get<PaginaDePessoas>(comParametros("/admin/relatorios/conformidade/pessoas", { ...filtro })),
    placeholderData: (anterior) => anterior,
  });
}

export function useDesempenho(filtro: FiltroDeDesempenho) {
  return useQuery({
    queryKey: chaves.desempenho(filtro),
    queryFn: () => api.get<Desempenho>(comParametros("/admin/relatorios/desempenho", { ...filtro })),
  });
}

export function useEngajamento(semanas: number) {
  return useQuery({
    queryKey: chaves.engajamento(semanas),
    queryFn: () => api.get<Engajamento>(comParametros("/admin/relatorios/engajamento", { semanas })),
  });
}

export function enderecoDaExportacao(
  relatorio: RelatorioExportavel,
  formato: FormatoDeExportacao,
  valores: Record<string, Valor> = {},
): string {
  return comParametros(`/api/admin/relatorios/${relatorio}/exportar`, { ...valores, formato });
}

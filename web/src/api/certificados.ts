import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ErroDaApi } from "./cliente";
import { parametros } from "./relatorios";
import type {
  FiltroDeCertificados,
  ModeloDoCertificado,
  ModuloComCertificado,
  PaginaDeCertificados,
  PosicaoDoCampo,
} from "./tipos";

const chaves = {
  emitidos: (filtro: FiltroDeCertificados) => ["admin", "certificados", "emitidos", filtro] as const,
  modulos: ["admin", "certificados", "modulos"] as const,
  modelo: ["admin", "certificados", "modelo"] as const,
};

function comParametros(caminho: string, valores: Record<string, string | number | undefined>): string {
  const consulta = parametros(valores);
  return consulta ? `${caminho}?${consulta}` : caminho;
}

export function useCertificadosEmitidos(filtro: FiltroDeCertificados) {
  return useQuery({
    queryKey: chaves.emitidos(filtro),
    queryFn: () => api.get<PaginaDeCertificados>(comParametros("/admin/certificados", { ...filtro })),
    placeholderData: (anterior) => anterior,
  });
}

export function useModulosComCertificado() {
  return useQuery({
    queryKey: chaves.modulos,
    queryFn: () => api.get<ModuloComCertificado[]>("/admin/certificados/modulos"),
  });
}

export function useLiberarCertificado() {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: ({ moduloId, liberado }: { moduloId: number; liberado: boolean }) =>
      api.post(`/admin/certificados/modulos/${moduloId}/liberacao`, { liberado }),
    onSuccess: () => cliente.invalidateQueries({ queryKey: ["admin"] }),
  });
}

export function useModeloDoCertificado() {
  return useQuery({
    queryKey: chaves.modelo,
    queryFn: () => api.get<ModeloDoCertificado>("/admin/certificados/modelo"),
  });
}

function useAtualizarModelo<T>(acao: (entrada: T) => Promise<ModeloDoCertificado>) {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: acao,
    onSuccess: (modelo) => cliente.setQueryData(chaves.modelo, modelo),
  });
}

export function useSalvarModelo() {
  return useAtualizarModelo((campos: PosicaoDoCampo[]) =>
    api.put<ModeloDoCertificado>("/admin/certificados/modelo", { campos }),
  );
}

export function useEnviarArte() {
  return useAtualizarModelo((arquivo: File) => {
    const formulario = new FormData();
    formulario.append("arquivo", arquivo);
    return api.post<ModeloDoCertificado>("/admin/certificados/modelo/arte", formulario);
  });
}

export function useRemoverArte() {
  return useAtualizarModelo(() => api.delete<ModeloDoCertificado>("/admin/certificados/modelo/arte"));
}

export function useBaixarExemplo() {
  return useMutation({
    mutationFn: async (campos: PosicaoDoCampo[]) => {
      const resposta = await fetch("/api/admin/certificados/modelo/previa", {
        method: "POST",
        credentials: "same-origin",
        headers: { Accept: "application/pdf, application/json", "Content-Type": "application/json" },
        body: JSON.stringify({ campos }),
      });
      if (!resposta.ok) {
        throw await erroDa(resposta);
      }
      salvarArquivo(await resposta.blob(), "exemplo-de-certificado.pdf");
    },
  });
}

export function enderecoDaArte(versao?: string): string {
  return `/api/admin/certificados/modelo/arte${versao ? `?v=${encodeURIComponent(versao)}` : ""}`;
}

export function enderecoDoZip(filtro: Omit<FiltroDeCertificados, "pagina">): string {
  return comParametros("/api/admin/certificados/exportar", { ...filtro });
}

async function erroDa(resposta: Response): Promise<ErroDaApi> {
  const problema = (await resposta.json().catch(() => null)) as { detail?: string } | null;
  return new ErroDaApi(
    resposta.status,
    problema?.detail ?? "Não foi possível falar com o servidor. Tente de novo.",
  );
}

function salvarArquivo(conteudo: Blob, nome: string) {
  const endereco = URL.createObjectURL(conteudo);
  const link = document.createElement("a");
  link.href = endereco;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(endereco);
}

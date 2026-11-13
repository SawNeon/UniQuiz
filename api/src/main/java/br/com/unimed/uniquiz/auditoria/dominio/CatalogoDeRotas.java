package br.com.unimed.uniquiz.auditoria.dominio;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class CatalogoDeRotas {

    private static final Map<String, AcaoAuditada> ACOES = Map.ofEntries(
            entry("POST /api/admin/modulos", AcaoAuditada.MODULO_CRIADO),
            entry("PUT /api/admin/modulos/{moduloId}", AcaoAuditada.MODULO_EDITADO),
            entry("POST /api/admin/modulos/{moduloId}/status", AcaoAuditada.MODULO_SITUACAO_ALTERADA),
            entry("PATCH /api/admin/modulos/ordem", AcaoAuditada.MODULOS_REORDENADOS),
            entry("POST /api/admin/modulos/{moduloId}/licoes", AcaoAuditada.LICAO_CRIADA),
            entry("PATCH /api/admin/modulos/{moduloId}/licoes/ordem", AcaoAuditada.LICOES_REORDENADAS),
            entry("PUT /api/admin/licoes/{licaoId}", AcaoAuditada.LICAO_EDITADA),
            entry("POST /api/admin/licoes/{licaoId}/status", AcaoAuditada.LICAO_SITUACAO_ALTERADA),
            entry("POST /api/admin/licoes/{licaoId}/desafios", AcaoAuditada.DESAFIO_CRIADO),
            entry("PATCH /api/admin/licoes/{licaoId}/desafios/ordem", AcaoAuditada.DESAFIOS_REORDENADOS),
            entry("PUT /api/admin/desafios/{desafioId}", AcaoAuditada.DESAFIO_EDITADO),
            entry("POST /api/admin/desafios/{desafioId}/status", AcaoAuditada.DESAFIO_SITUACAO_ALTERADA),
            entry("POST /api/admin/desafios/{desafioId}/duplicar", AcaoAuditada.DESAFIO_DUPLICADO),
            entry("POST /api/admin/colaboradores", AcaoAuditada.COLABORADOR_CADASTRADO),
            entry("PATCH /api/admin/colaboradores/{colaboradorId}", AcaoAuditada.COLABORADOR_AJUSTADO),
            entry("POST /api/admin/colaboradores/{colaboradorId}/redefinir-pin", AcaoAuditada.PIN_REDEFINIDO),
            entry("POST /api/admin/colaboradores/importacao/confirmar", AcaoAuditada.PLANILHA_IMPORTADA),
            entry("POST /api/admin/colaboradores/{colaboradorId}/zerar-pontos", AcaoAuditada.PONTOS_ZERADOS),
            entry("POST /api/admin/temporadas", AcaoAuditada.TEMPORADA_CRIADA),
            entry("PUT /api/admin/temporadas/{temporadaId}", AcaoAuditada.TEMPORADA_EDITADA),
            entry("POST /api/admin/usuarios", AcaoAuditada.USUARIO_DO_PAINEL_CRIADO),
            entry("PUT /api/admin/usuarios/{usuarioId}", AcaoAuditada.USUARIO_DO_PAINEL_EDITADO),
            entry("POST /api/admin/usuarios/{usuarioId}/desativar", AcaoAuditada.USUARIO_DO_PAINEL_DESATIVADO),
            entry("POST /api/admin/usuarios/{usuarioId}/reativar", AcaoAuditada.USUARIO_DO_PAINEL_REATIVADO),
            entry("POST /api/admin/usuarios/{usuarioId}/desbloquear", AcaoAuditada.USUARIO_DO_PAINEL_DESBLOQUEADO),
            entry("POST /api/admin/usuarios/{usuarioId}/redefinir-senha", AcaoAuditada.SENHA_REDEFINIDA),
            entry("POST /api/admin/usuarios/{usuarioId}/encerrar-sessoes", AcaoAuditada.SESSOES_DE_USUARIO_ENCERRADAS),
            entry("POST /api/admin/minha-conta/senha", AcaoAuditada.SENHA_TROCADA),
            entry("DELETE /api/admin/minha-conta/sessoes/{sessaoId}", AcaoAuditada.SESSAO_ENCERRADA),
            entry("POST /api/admin/minha-conta/sessoes/encerrar-outras", AcaoAuditada.OUTRAS_SESSOES_ENCERRADAS));

    private static final Set<String> REGISTRADAS_PELO_PROPRIO_SERVICO = Set.of(
            "POST /api/admin/login", "DELETE /api/admin/sessao", "POST /api/admin/colaboradores/importacao/previa");

    private CatalogoDeRotas() {}

    public static boolean registradaPeloInterceptador(String metodo, String rota) {
        return !REGISTRADAS_PELO_PROPRIO_SERVICO.contains(chave(metodo, rota));
    }

    public static boolean conhecida(String metodo, String rota) {
        String chave = chave(metodo, rota);
        return ACOES.containsKey(chave) || REGISTRADAS_PELO_PROPRIO_SERVICO.contains(chave);
    }

    public static AcaoAuditada acaoDa(String metodo, String rota) {
        return ACOES.getOrDefault(chave(metodo, rota), AcaoAuditada.ACAO_NAO_CATALOGADA);
    }

    public static String alvoDe(Map<String, String> variaveisDaRota) {
        if (variaveisDaRota == null || variaveisDaRota.isEmpty()) {
            return null;
        }
        return new TreeMap<>(variaveisDaRota)
                .entrySet().stream()
                        .map(variavel -> variavel.getKey() + "=" + variavel.getValue())
                        .collect(Collectors.joining(", "));
    }

    private static String chave(String metodo, String rota) {
        return metodo + " " + rota;
    }
}

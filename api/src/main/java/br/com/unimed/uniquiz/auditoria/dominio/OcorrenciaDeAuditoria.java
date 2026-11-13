package br.com.unimed.uniquiz.auditoria.dominio;

public record OcorrenciaDeAuditoria(
        TipoDeAtor ator,
        Long atorId,
        String atorDescricao,
        AcaoAuditada acao,
        String alvo,
        ResultadoDaAcao resultado,
        String detalhe) {

    private static final int LIMITE_DA_DESCRICAO = 200;
    private static final int LIMITE_DO_ALVO = 300;
    private static final int LIMITE_DO_DETALHE = 500;

    public OcorrenciaDeAuditoria {
        atorDescricao = limitar(atorDescricao, LIMITE_DA_DESCRICAO);
        alvo = limitar(alvo, LIMITE_DO_ALVO);
        detalhe = limitar(detalhe, LIMITE_DO_DETALHE);
    }

    public static OcorrenciaDeAuditoria de(TipoDeAtor ator, Long atorId, AcaoAuditada acao) {
        return new OcorrenciaDeAuditoria(ator, atorId, null, acao, null, ResultadoDaAcao.SUCESSO, null);
    }

    public OcorrenciaDeAuditoria descrevendoOAtor(String descricao) {
        return new OcorrenciaDeAuditoria(ator, atorId, descricao, acao, alvo, resultado, detalhe);
    }

    public OcorrenciaDeAuditoria comAlvo(String novoAlvo) {
        return new OcorrenciaDeAuditoria(ator, atorId, atorDescricao, acao, novoAlvo, resultado, detalhe);
    }

    public OcorrenciaDeAuditoria comDetalhe(String novoDetalhe) {
        return new OcorrenciaDeAuditoria(ator, atorId, atorDescricao, acao, alvo, resultado, novoDetalhe);
    }

    public OcorrenciaDeAuditoria comFalha(String motivo) {
        return new OcorrenciaDeAuditoria(ator, atorId, atorDescricao, acao, alvo, ResultadoDaAcao.FALHA, motivo);
    }

    private static String limitar(String texto, int limite) {
        if (texto == null || texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, limite);
    }
}

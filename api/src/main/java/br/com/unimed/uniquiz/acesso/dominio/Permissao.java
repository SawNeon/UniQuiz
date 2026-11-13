package br.com.unimed.uniquiz.acesso.dominio;

public enum Permissao {
    GERIR_CONTEUDO,
    GERIR_COLABORADORES,
    GERIR_TEMPORADAS,
    GERIR_USUARIOS_DO_PAINEL,
    VER_RELATORIOS,
    EXPORTAR_RELATORIOS,
    VER_AUDITORIA;

    public static final String PREFIXO_DA_AUTORIDADE = "PERM_";

    public String autoridade() {
        return PREFIXO_DA_AUTORIDADE + name();
    }
}

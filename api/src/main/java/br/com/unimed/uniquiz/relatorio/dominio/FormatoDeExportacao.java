package br.com.unimed.uniquiz.relatorio.dominio;

public enum FormatoDeExportacao {
    CSV("csv", "text/csv; charset=UTF-8"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extensao;
    private final String tipoDeConteudo;

    FormatoDeExportacao(String extensao, String tipoDeConteudo) {
        this.extensao = extensao;
        this.tipoDeConteudo = tipoDeConteudo;
    }

    public String extensao() {
        return extensao;
    }

    public String tipoDeConteudo() {
        return tipoDeConteudo;
    }
}

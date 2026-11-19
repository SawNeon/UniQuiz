package br.com.unimed.uniquiz.certificado.dominio;

public record PosicaoDoCampo(
        CampoDoCertificado campo,
        boolean visivel,
        double x,
        double y,
        double tamanho,
        String cor,
        Alinhamento alinhamento,
        boolean negrito,
        double larguraMaxima,
        String prefixo) {

    public enum Alinhamento {
        ESQUERDA,
        CENTRO,
        DIREITA
    }

    public String prefixoOuVazio() {
        return prefixo == null ? "" : prefixo;
    }
}

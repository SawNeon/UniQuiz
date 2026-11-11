package br.com.unimed.uniquiz.conteudo.dominio;

public record Correcao(boolean correta, String motivoDoErro, String respostaCerta) {

    public static Correcao acerto(String respostaCerta) {
        return new Correcao(true, null, respostaCerta);
    }

    public static Correcao erro(String motivoDoErro, String respostaCerta) {
        return new Correcao(false, motivoDoErro, respostaCerta);
    }
}

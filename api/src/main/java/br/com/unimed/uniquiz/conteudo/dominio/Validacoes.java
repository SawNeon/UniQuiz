package br.com.unimed.uniquiz.conteudo.dominio;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;

final class Validacoes {

    private Validacoes() {}

    static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new ErroDeNegocio(mensagem);
        }
    }

    static boolean preenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    static <T> T respostaDoTipo(RespostaDoColaborador resposta, Class<T> tipo) {
        if (!tipo.isInstance(resposta)) {
            throw new ErroDeNegocio("A resposta não corresponde ao tipo do desafio.");
        }
        return tipo.cast(resposta);
    }
}

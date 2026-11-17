package br.com.unimed.uniquiz.relatorio.dominio;

import br.com.unimed.uniquiz.conteudo.dominio.ConteudoCompletarClicando;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoDesafio;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoVerdadeiroOuFalso;
import java.util.regex.Pattern;

public final class TextoDoDesafio {

    private static final Pattern LACUNA = Pattern.compile("\\{\\{\\d+}}");

    private TextoDoDesafio() {}

    public static String descrever(String enunciado, ConteudoDesafio conteudo) {
        String especifico =
                switch (conteudo) {
                    case ConteudoVerdadeiroOuFalso verdadeiroOuFalso -> verdadeiroOuFalso.afirmacao();
                    case ConteudoCompletarClicando completar ->
                        completar.texto() == null
                                ? null
                                : LACUNA.matcher(completar.texto()).replaceAll("___");
                    case null, default -> null;
                };
        return especifico == null || especifico.isBlank() ? enunciado : especifico.strip();
    }
}

package br.com.unimed.uniquiz.relatorio.dominio;

import br.com.unimed.uniquiz.conteudo.dominio.ConteudoCompletarClicando;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoDesafio;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoMultiplaEscolha;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoVerdadeiroOuFalso;
import br.com.unimed.uniquiz.conteudo.dominio.RespostaDoColaborador;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class DescricaoDaResposta {

    private DescricaoDaResposta() {}

    public static String descrever(ConteudoDesafio conteudo, RespostaDoColaborador resposta) {
        return switch (conteudo) {
            case ConteudoMultiplaEscolha multipla
            when resposta instanceof RespostaDoColaborador.MultiplaEscolha escolha ->
                multipla.alternativas().stream()
                        .filter(alternativa -> alternativa.id().equals(escolha.alternativaId()))
                        .map(ConteudoMultiplaEscolha.Alternativa::texto)
                        .findFirst()
                        .orElse("Alternativa removida");
            case ConteudoVerdadeiroOuFalso ignorado
            when resposta instanceof RespostaDoColaborador.VerdadeiroOuFalso valor ->
                Boolean.TRUE.equals(valor.valor()) ? "Verdadeiro" : "Falso";
            case ConteudoCompletarClicando ignorado
            when resposta instanceof RespostaDoColaborador.CompletarClicando lacunas ->
                palavrasEmOrdem(lacunas.lacunas());
            default -> "Resposta em formato antigo";
        };
    }

    private static String palavrasEmOrdem(Map<Integer, String> lacunas) {
        if (lacunas == null || lacunas.isEmpty()) {
            return "Sem palavras";
        }
        return new TreeMap<>(lacunas).values().stream().collect(Collectors.joining(" / "));
    }
}

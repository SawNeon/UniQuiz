package br.com.unimed.uniquiz.conteudo.dominio;

import java.util.Map;

public sealed interface RespostaDoColaborador {

    record MultiplaEscolha(String alternativaId) implements RespostaDoColaborador {}

    record VerdadeiroOuFalso(Boolean valor) implements RespostaDoColaborador {}

    record CompletarClicando(Map<Integer, String> lacunas) implements RespostaDoColaborador {}
}

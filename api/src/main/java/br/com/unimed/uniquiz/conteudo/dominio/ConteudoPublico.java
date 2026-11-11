package br.com.unimed.uniquiz.conteudo.dominio;

import java.util.List;

public sealed interface ConteudoPublico {

    record MultiplaEscolha(List<AlternativaPublica> alternativas) implements ConteudoPublico {}

    record AlternativaPublica(String id, String texto) {}

    record VerdadeiroOuFalso(String afirmacao) implements ConteudoPublico {}

    record CompletarClicando(String texto, List<LacunaPublica> lacunas) implements ConteudoPublico {}

    record LacunaPublica(int id, List<String> palavras) {}
}

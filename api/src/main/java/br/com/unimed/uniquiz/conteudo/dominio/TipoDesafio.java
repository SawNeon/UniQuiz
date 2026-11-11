package br.com.unimed.uniquiz.conteudo.dominio;

public enum TipoDesafio {
    MULTIPLA_ESCOLHA(ConteudoMultiplaEscolha.class, RespostaDoColaborador.MultiplaEscolha.class),
    VERDADEIRO_OU_FALSO(ConteudoVerdadeiroOuFalso.class, RespostaDoColaborador.VerdadeiroOuFalso.class),
    COMPLETAR_CLICANDO(ConteudoCompletarClicando.class, RespostaDoColaborador.CompletarClicando.class);

    private final Class<? extends ConteudoDesafio> classeDoConteudo;
    private final Class<? extends RespostaDoColaborador> classeDaResposta;

    TipoDesafio(
            Class<? extends ConteudoDesafio> classeDoConteudo,
            Class<? extends RespostaDoColaborador> classeDaResposta) {
        this.classeDoConteudo = classeDoConteudo;
        this.classeDaResposta = classeDaResposta;
    }

    public Class<? extends ConteudoDesafio> classeDoConteudo() {
        return classeDoConteudo;
    }

    public Class<? extends RespostaDoColaborador> classeDaResposta() {
        return classeDaResposta;
    }
}

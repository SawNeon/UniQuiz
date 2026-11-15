package br.com.unimed.uniquiz.relatorio.dominio;

import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;

public record CalibracaoDaDificuldade(Dificuldade observada, Avaliacao avaliacao) {

    public static final int RESPOSTAS_MINIMAS = 10;
    private static final double ACERTO_DE_DESAFIO_FACIL = 0.8;
    private static final double ACERTO_DE_DESAFIO_MEDIO = 0.5;

    public enum Avaliacao {
        ADEQUADA,
        MAIS_FACIL_QUE_O_PREVISTO,
        MAIS_DIFICIL_QUE_O_PREVISTO,
        POUCOS_DADOS
    }

    public static CalibracaoDaDificuldade calibrar(Dificuldade declarada, long respostas, long acertos) {
        if (respostas < RESPOSTAS_MINIMAS) {
            return new CalibracaoDaDificuldade(null, Avaliacao.POUCOS_DADOS);
        }
        double taxa = (double) acertos / respostas;
        Dificuldade observada;
        if (taxa >= ACERTO_DE_DESAFIO_FACIL) {
            observada = Dificuldade.FACIL;
        } else if (taxa >= ACERTO_DE_DESAFIO_MEDIO) {
            observada = Dificuldade.MEDIO;
        } else {
            observada = Dificuldade.DIFICIL;
        }
        int diferenca = observada.compareTo(declarada);
        Avaliacao avaliacao;
        if (diferenca == 0) {
            avaliacao = Avaliacao.ADEQUADA;
        } else if (diferenca < 0) {
            avaliacao = Avaliacao.MAIS_FACIL_QUE_O_PREVISTO;
        } else {
            avaliacao = Avaliacao.MAIS_DIFICIL_QUE_O_PREVISTO;
        }
        return new CalibracaoDaDificuldade(observada, avaliacao);
    }
}

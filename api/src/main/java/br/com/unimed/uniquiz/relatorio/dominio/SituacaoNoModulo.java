package br.com.unimed.uniquiz.relatorio.dominio;

public enum SituacaoNoModulo {
    CONCLUIU("Concluiu"),
    EM_ANDAMENTO("Em andamento"),
    NAO_COMECOU("Não começou");

    private final String rotulo;

    SituacaoNoModulo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public static SituacaoNoModulo de(long licoesConcluidas, long totalDeLicoes) {
        if (licoesConcluidas <= 0) {
            return NAO_COMECOU;
        }
        return licoesConcluidas >= totalDeLicoes ? CONCLUIU : EM_ANDAMENTO;
    }
}

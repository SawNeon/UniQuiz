package br.com.unimed.uniquiz.conteudo.dominio;

import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.exigir;

public record QuantidadesPorNivel(int facil, int medio, int dificil) {

    public static final int MAXIMO_POR_NIVEL = 30;

    public static final QuantidadesPorNivel NENHUMA = new QuantidadesPorNivel(0, 0, 0);

    public QuantidadesPorNivel {
        exigir(
                dentroDoLimite(facil) && dentroDoLimite(medio) && dentroDoLimite(dificil),
                "Cada nível aceita de 0 a %d desafios por pessoa.".formatted(MAXIMO_POR_NIVEL));
    }

    public int total() {
        return facil + medio + dificil;
    }

    public int de(Dificuldade nivel) {
        return switch (nivel) {
            case FACIL -> facil;
            case MEDIO -> medio;
            case DIFICIL -> dificil;
        };
    }

    private static boolean dentroDoLimite(int quantidade) {
        return quantidade >= 0 && quantidade <= MAXIMO_POR_NIVEL;
    }
}

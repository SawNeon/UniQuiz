package br.com.unimed.uniquiz.jogo.dominio;

import br.com.unimed.uniquiz.classificacao.dominio.Ganho;
import br.com.unimed.uniquiz.classificacao.dominio.MotivoDosPontos;
import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import java.util.ArrayList;
import java.util.List;

public record RegrasDePontuacao(
        double multiplicadorFacil,
        double multiplicadorMedio,
        double multiplicadorDificil,
        int acertosParaBonus,
        int bonusSequenciaAcertos,
        int bonusLicaoPerfeita,
        int bonusPrimeiraLicaoDoDia,
        int pontosPorNivel) {

    public static final int PERCENTUAL_INTEGRAL = 100;

    public record PontosDoAcerto(int base, int bonusDeSequencia) {

        public int total() {
            return base + bonusDeSequencia;
        }
    }

    public PontosDoAcerto pontosPorAcerto(
            int pontosDoDesafio, Dificuldade dificuldade, int acertosSeguidos, int percentual) {
        int base = (int) Math.round(pontosDoDesafio * multiplicador(dificuldade));
        int bonus = acertosSeguidos >= acertosParaBonus ? bonusSequenciaAcertos : 0;
        return new PontosDoAcerto(aplicar(base, percentual), aplicar(bonus, percentual));
    }

    public List<Ganho> bonusDeConclusao(boolean perfeita, boolean primeiraDoDia, int percentual) {
        List<Ganho> ganhos = new ArrayList<>();
        if (perfeita) {
            ganhos.add(new Ganho(MotivoDosPontos.LICAO_PERFEITA, aplicar(bonusLicaoPerfeita, percentual)));
        }
        if (primeiraDoDia) {
            ganhos.add(new Ganho(MotivoDosPontos.PRIMEIRA_LICAO_DO_DIA, aplicar(bonusPrimeiraLicaoDoDia, percentual)));
        }
        return ganhos.stream().filter(ganho -> ganho.pontos() > 0).toList();
    }

    public int nivel(int pontosTotais) {
        return Math.max(pontosTotais, 0) / pontosPorNivel + 1;
    }

    public static int percentualAoRefazer(long conclusoesAnteriores) {
        if (conclusoesAnteriores == 0) {
            return PERCENTUAL_INTEGRAL;
        }
        return conclusoesAnteriores == 1 ? 50 : 0;
    }

    private double multiplicador(Dificuldade dificuldade) {
        return switch (dificuldade) {
            case FACIL -> multiplicadorFacil;
            case MEDIO -> multiplicadorMedio;
            case DIFICIL -> multiplicadorDificil;
        };
    }

    private static int aplicar(int pontos, int percentual) {
        return pontos * percentual / PERCENTUAL_INTEGRAL;
    }
}

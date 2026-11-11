package br.com.unimed.uniquiz.jogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.classificacao.dominio.Ganho;
import br.com.unimed.uniquiz.classificacao.dominio.MotivoDosPontos;
import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegrasDePontuacaoTest {

    private final RegrasDePontuacao regras = new RegrasDePontuacao(1.0, 1.5, 2.0, 3, 5, 20, 10, 500);

    @ParameterizedTest
    @CsvSource({"FACIL, 10", "MEDIO, 15", "DIFICIL, 20"})
    void aplicaOMultiplicadorDaDificuldade(Dificuldade dificuldade, int esperado) {
        assertThat(regras.pontosPorAcerto(10, dificuldade, 1, 100).base()).isEqualTo(esperado);
    }

    @Test
    void bonusDeSequenciaComecaNoTerceiroAcertoSeguido() {
        assertThat(regras.pontosPorAcerto(10, Dificuldade.FACIL, 2, 100).bonusDeSequencia())
                .isZero();
        assertThat(regras.pontosPorAcerto(10, Dificuldade.FACIL, 3, 100).bonusDeSequencia())
                .isEqualTo(5);
        assertThat(regras.pontosPorAcerto(10, Dificuldade.FACIL, 4, 100).bonusDeSequencia())
                .isEqualTo(5);
    }

    @Test
    void refazerValeMetadeUmaVezEDepoisNada() {
        assertThat(RegrasDePontuacao.percentualAoRefazer(0)).isEqualTo(100);
        assertThat(RegrasDePontuacao.percentualAoRefazer(1)).isEqualTo(50);
        assertThat(RegrasDePontuacao.percentualAoRefazer(2)).isZero();
        assertThat(regras.pontosPorAcerto(10, Dificuldade.MEDIO, 1, 50).base()).isEqualTo(7);
    }

    @Test
    void bonusDeConclusao() {
        assertThat(regras.bonusDeConclusao(true, true, 100))
                .containsExactly(
                        new Ganho(MotivoDosPontos.LICAO_PERFEITA, 20),
                        new Ganho(MotivoDosPontos.PRIMEIRA_LICAO_DO_DIA, 10));
        assertThat(regras.bonusDeConclusao(false, false, 100)).isEmpty();
        assertThat(regras.bonusDeConclusao(true, true, 0)).isEmpty();
    }

    @Test
    void nivelSobeACadaQuinhentosPontos() {
        assertThat(regras.nivel(0)).isEqualTo(1);
        assertThat(regras.nivel(499)).isEqualTo(1);
        assertThat(regras.nivel(500)).isEqualTo(2);
        assertThat(regras.nivel(-30)).isEqualTo(1);
    }
}

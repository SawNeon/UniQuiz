package br.com.unimed.uniquiz.trofeu.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.trofeu.dominio.RegrasDeTrofeus.Situacao;
import org.junit.jupiter.api.Test;

class RegrasDeTrofeusTest {

    @Test
    void primeiraLicaoComErrosDaSoOPrimeiroPasso() {
        assertThat(RegrasDeTrofeus.merecidos(new Situacao(true, false, 1, false, false)))
                .containsExactly(CodigoDoTrofeu.PRIMEIRO_PASSO);
    }

    @Test
    void sequenciasDeTresESeteDias() {
        assertThat(RegrasDeTrofeus.merecidos(new Situacao(true, false, 3, false, false)))
                .contains(CodigoDoTrofeu.SEQUENCIA_3)
                .doesNotContain(CodigoDoTrofeu.SEQUENCIA_7);
        assertThat(RegrasDeTrofeus.merecidos(new Situacao(true, false, 7, false, false)))
                .contains(CodigoDoTrofeu.SEQUENCIA_3, CodigoDoTrofeu.SEQUENCIA_7);
    }

    @Test
    void tudoAoMesmoTempo() {
        assertThat(RegrasDeTrofeus.merecidos(new Situacao(true, true, 7, true, true)))
                .containsExactlyInAnyOrder(CodigoDoTrofeu.values());
    }

    @Test
    void semConcluirNadaNaoGanhaTrofeu() {
        assertThat(RegrasDeTrofeus.merecidos(new Situacao(false, false, 0, false, false)))
                .isEmpty();
    }
}

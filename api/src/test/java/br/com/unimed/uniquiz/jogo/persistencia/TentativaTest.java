package br.com.unimed.uniquiz.jogo.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TentativaTest {

    private static final Instant INICIO = Instant.parse("2026-11-10T12:00:00Z");

    @Test
    void concluirMedeOTempoNoServidor() {
        Tentativa tentativa = new Tentativa(1L, 2L, 3L, INICIO, 5, 100);

        tentativa.concluir(INICIO.plusSeconds(95), true);

        assertThat(tentativa.getTempoMs()).isEqualTo(95_000);
    }

    @Test
    void naoAceitaNadaDepoisDeConcluidaOuAbandonada() {
        Tentativa concluida = new Tentativa(1L, 2L, null, INICIO, 5, 100);
        concluida.concluir(INICIO.plusSeconds(1), false);
        Tentativa abandonada = new Tentativa(1L, 2L, null, INICIO, 5, 100);
        abandonada.abandonar();

        assertThatThrownBy(concluida::exigirEmAndamento).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> concluida.concluir(INICIO.plusSeconds(2), true))
                .isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(abandonada::exigirEmAndamento).isInstanceOf(ErroDeNegocio.class);
    }

    @Test
    void contaOsAcertos() {
        Tentativa tentativa = new Tentativa(1L, 2L, null, INICIO, 5, 50);
        tentativa.registrarAcerto();
        tentativa.registrarAcerto();

        assertThat(tentativa.getAcertos()).isEqualTo(2);
        assertThat(tentativa.getPercentualPontos()).isEqualTo(50);
    }
}

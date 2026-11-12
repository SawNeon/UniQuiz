package br.com.unimed.uniquiz.colaborador.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ColaboradorTest {

    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");

    private Colaborador colaborador() {
        return new Colaborador(1L, 1L, "Ana Exemplo", AGORA);
    }

    @Test
    void comecaSemPin() {
        assertThat(colaborador().temPin()).isFalse();
    }

    @Test
    void bloqueiaNaQuintaFalhaPorQuinzeMinutos() {
        Colaborador colaborador = colaborador();
        colaborador.definirPin("hash");

        for (int falha = 1; falha < Colaborador.TENTATIVAS_DE_PIN_ANTES_DO_BLOQUEIO; falha++) {
            colaborador.registrarFalhaDePin(AGORA);
            assertThat(colaborador.pinBloqueado(AGORA)).isFalse();
        }
        colaborador.registrarFalhaDePin(AGORA);

        assertThat(colaborador.pinBloqueado(AGORA)).isTrue();
        assertThat(colaborador.pinBloqueado(AGORA.plus(Colaborador.TEMPO_DE_BLOQUEIO_DO_PIN)))
                .isFalse();
    }

    @Test
    void acertoZeraAsFalhas() {
        Colaborador colaborador = colaborador();
        colaborador.definirPin("hash");
        colaborador.registrarFalhaDePin(AGORA);
        colaborador.registrarFalhaDePin(AGORA);

        colaborador.registrarAcertoDePin();
        for (int falha = 1; falha < Colaborador.TENTATIVAS_DE_PIN_ANTES_DO_BLOQUEIO; falha++) {
            colaborador.registrarFalhaDePin(AGORA);
        }

        assertThat(colaborador.pinBloqueado(AGORA)).isFalse();
    }

    @Test
    void removerPinDestravaEApagaOHash() {
        Colaborador colaborador = colaborador();
        colaborador.definirPin("hash");
        for (int falha = 0; falha < Colaborador.TENTATIVAS_DE_PIN_ANTES_DO_BLOQUEIO; falha++) {
            colaborador.registrarFalhaDePin(AGORA);
        }

        colaborador.removerPin();

        assertThat(colaborador.temPin()).isFalse();
        assertThat(colaborador.pinBloqueado(AGORA)).isFalse();
    }
}

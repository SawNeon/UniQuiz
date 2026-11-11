package br.com.unimed.uniquiz.acesso.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdministradorTest {

    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");

    @Test
    void bloqueiaNaQuintaFalhaPorQuinzeMinutos() {
        Administrador administrador = new Administrador("admin", "hash");

        for (int falha = 1; falha < Administrador.TENTATIVAS_ANTES_DO_BLOQUEIO; falha++) {
            administrador.registrarFalha(AGORA);
            assertThat(administrador.bloqueado(AGORA)).isFalse();
        }
        administrador.registrarFalha(AGORA);

        assertThat(administrador.bloqueado(AGORA)).isTrue();
        assertThat(administrador.bloqueado(
                        AGORA.plus(Administrador.TEMPO_DE_BLOQUEIO).minusSeconds(1)))
                .isTrue();
        assertThat(administrador.bloqueado(AGORA.plus(Administrador.TEMPO_DE_BLOQUEIO)))
                .isFalse();
    }

    @Test
    void acertoZeraAsFalhas() {
        Administrador administrador = new Administrador("admin", "hash");
        for (int falha = 1; falha < Administrador.TENTATIVAS_ANTES_DO_BLOQUEIO; falha++) {
            administrador.registrarFalha(AGORA);
        }

        administrador.registrarSucesso();
        administrador.registrarFalha(AGORA);

        assertThat(administrador.bloqueado(AGORA)).isFalse();
    }
}

package br.com.unimed.uniquiz.acesso.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.acesso.persistencia.Sessao.LimitesDaSessao;
import br.com.unimed.uniquiz.acesso.persistencia.Sessao.Origem;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SessaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-13T12:00:00Z");
    private static final Origem ORIGEM = new Origem("10.0.0.1", "Chrome no Windows");
    private static final String HASH = "a".repeat(64);

    @Test
    void sessaoDoPainelCaiDepoisDeTrintaMinutosParada() {
        Sessao sessao = Sessao.deAdministrador(
                HASH, 1L, AGORA, new LimitesDaSessao(Duration.ofHours(8), Duration.ofMinutes(30)), ORIGEM);

        assertThat(sessao.expirada(AGORA.plus(Duration.ofMinutes(29)))).isFalse();
        assertThat(sessao.expirada(AGORA.plus(Duration.ofMinutes(30)))).isTrue();
    }

    @Test
    void usoRenovaAInatividadeMasNaoPassaDoLimiteAbsoluto() {
        Sessao sessao = Sessao.deAdministrador(
                HASH, 1L, AGORA, new LimitesDaSessao(Duration.ofHours(1), Duration.ofMinutes(30)), ORIGEM);

        sessao.registrarUso(AGORA.plus(Duration.ofMinutes(25)));
        assertThat(sessao.expirada(AGORA.plus(Duration.ofMinutes(50)))).isFalse();

        sessao.registrarUso(AGORA.plus(Duration.ofMinutes(50)));
        assertThat(sessao.expirada(AGORA.plus(Duration.ofMinutes(60)))).isTrue();
    }

    @Test
    void registraOUsoNoMaximoUmaVezPorMinuto() {
        Sessao sessao = Sessao.deAdministrador(
                HASH, 1L, AGORA, new LimitesDaSessao(Duration.ofHours(8), Duration.ofMinutes(30)), ORIGEM);

        assertThat(sessao.registrarUso(AGORA.plusSeconds(30))).isFalse();
        assertThat(sessao.getUltimoUsoEm()).isEqualTo(AGORA);
        assertThat(sessao.registrarUso(AGORA.plusSeconds(60))).isTrue();
        assertThat(sessao.getUltimoUsoEm()).isEqualTo(AGORA.plusSeconds(60));
    }

    @Test
    void sessaoDoColaboradorSemInatividadeSoExpiraNoFim() {
        Sessao sessao = Sessao.deColaborador(HASH, 7L, AGORA, new LimitesDaSessao(Duration.ofDays(30), null), ORIGEM);

        assertThat(sessao.expirada(AGORA.plus(Duration.ofDays(29)))).isFalse();
        assertThat(sessao.expirada(AGORA.plus(Duration.ofDays(30)))).isTrue();
    }

    @Test
    void identificadorPublicoNaoExpoeOHashInteiro() {
        Sessao sessao = Sessao.deAdministrador(
                "0123456789abcdef" + "f".repeat(48), 1L, AGORA, new LimitesDaSessao(Duration.ofHours(8), null), ORIGEM);

        assertThat(sessao.identificadorPublico()).isEqualTo("0123456789abcdef");
        assertThat(sessao.getDispositivo()).isEqualTo("Chrome no Windows");
        assertThat(sessao.getIp()).isEqualTo("10.0.0.1");
    }
}

package br.com.unimed.uniquiz.classificacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TemporadaTest {

    private static final LocalDate PRIMEIRO_DE_DEZEMBRO = LocalDate.of(2026, 12, 1);
    private static final LocalDate TRINTA_E_UM = LocalDate.of(2026, 12, 31);

    private final Temporada dezembro = new Temporada(" Dezembro ", PRIMEIRO_DE_DEZEMBRO, TRINTA_E_UM, " ");

    @Test
    void limpaONomeEGuardaPremioVazioComoNulo() {
        assertThat(dezembro.getNome()).isEqualTo("Dezembro");
        assertThat(dezembro.getPremioTexto()).isNull();
    }

    @Test
    void recusaFimAntesDoInicio() {
        assertThatThrownBy(() -> new Temporada("Errada", TRINTA_E_UM, PRIMEIRO_DE_DEZEMBRO, null))
                .isInstanceOf(ErroDeNegocio.class);
    }

    @Test
    void detectaSobreposicaoInclusiveNasBordas() {
        assertThat(dezembro.sobrepoe(LocalDate.of(2026, 11, 1), PRIMEIRO_DE_DEZEMBRO))
                .isTrue();
        assertThat(dezembro.sobrepoe(TRINTA_E_UM, LocalDate.of(2027, 1, 31))).isTrue();
        assertThat(dezembro.sobrepoe(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 20)))
                .isTrue();
        assertThat(dezembro.sobrepoe(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .isFalse();
        assertThat(dezembro.sobrepoe(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)))
                .isFalse();
    }
}

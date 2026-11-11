package br.com.unimed.uniquiz.certificado.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CodigoDeValidacaoTest {

    @Test
    void geraCodigoValidoSemCaracteresAmbiguos() {
        String codigo = CodigoDeValidacao.gerar(new Random(42));

        assertThat(codigo).hasSize(CodigoDeValidacao.TAMANHO).doesNotContain("0", "O", "1", "I");
        assertThat(CodigoDeValidacao.valido(codigo)).isTrue();
    }

    @Test
    void aceitaOCodigoDigitadoComHifenEMinusculas() {
        String codigo = CodigoDeValidacao.gerar(new Random(7));
        String digitado = CodigoDeValidacao.formatado(codigo).toLowerCase();

        assertThat(CodigoDeValidacao.normalizar(digitado)).isEqualTo(codigo);
    }

    @Test
    void recusaFormatoInvalido() {
        assertThat(CodigoDeValidacao.valido("ABC")).isFalse();
        assertThat(CodigoDeValidacao.valido("O0O0O0O0O0O0")).isFalse();
    }
}

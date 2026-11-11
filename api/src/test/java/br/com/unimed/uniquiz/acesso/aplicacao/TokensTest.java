package br.com.unimed.uniquiz.acesso.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokensTest {

    @Test
    void tokensSaoLongosUnicosESeguroParaCookie() {
        Set<String> gerados = new HashSet<>();
        for (int vez = 0; vez < 1000; vez++) {
            gerados.add(Tokens.novo());
        }

        assertThat(gerados).hasSize(1000);
        assertThat(gerados.iterator().next()).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hashEDeterministicoEEmHexadecimal() {
        String token = Tokens.novo();

        assertThat(Tokens.hash(token)).isEqualTo(Tokens.hash(token)).hasSize(64).matches("[0-9a-f]+");
        assertThat(Tokens.hash(token)).isNotEqualTo(Tokens.hash(Tokens.novo()));
    }
}

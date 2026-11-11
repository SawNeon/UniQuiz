package br.com.unimed.uniquiz.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextosTest {

    @ParameterizedTest
    @CsvSource({
        "'  João   da  Silva ', 'JOAO DA SILVA'",
        "'Tecnologia da Informação', 'TECNOLOGIA DA INFORMACAO'",
        "'ÁGUA', 'AGUA'",
        "'', ''"
    })
    void normalizarIgnoraAcentoEspacoEMaiuscula(String entrada, String esperado) {
        assertThat(Textos.normalizar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"'  Ana   Lima ', 'Ana Lima'", "'Ana', 'Ana'"})
    void limparJuntaEspacosMasMantemAcentosEMaiusculas(String entrada, String esperado) {
        assertThat(Textos.limpar(entrada)).isEqualTo(esperado);
    }

    @org.junit.jupiter.api.Test
    void nuloViraTextoVazio() {
        assertThat(Textos.limpar(null)).isEmpty();
        assertThat(Textos.normalizar(null)).isEmpty();
    }
}

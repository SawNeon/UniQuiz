package br.com.unimed.uniquiz.acesso.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DescricaoDoDispositivoTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36|Chrome no Windows",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36 Edg/130.0|Edge no Windows",
                "Mozilla/5.0 (Linux; Android 14; SM-X200) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0 Safari/537.36|Samsung Internet no Android",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1|Safari no iPhone",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.5; rv:128.0) Gecko/20100101 Firefox/128.0|Firefox no macOS",
                "curl/8.4.0|Aparelho desconhecido",
                "|Aparelho desconhecido"
            })
    void descreveNavegadorESistemaDeFormaAmigavel(String navegador, String esperado) {
        assertThat(DescricaoDoDispositivo.de(navegador)).isEqualTo(esperado);
    }
}

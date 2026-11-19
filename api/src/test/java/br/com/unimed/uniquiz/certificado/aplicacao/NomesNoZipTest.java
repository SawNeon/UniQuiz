package br.com.unimed.uniquiz.certificado.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.certificado.persistencia.ConsultasDeCertificados.Emitido;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NomesNoZipTest {

    @Test
    void separaPorModuloETiraCaracteresQueOWindowsNaoAceita() {
        Emitido certificado = new Emitido(
                "ABCD2345EFGH",
                1,
                "LGPD: o básico / parte 1",
                2,
                "Ana \"Aninha\" <Exemplo>?",
                "Unimed",
                "TI",
                Instant.parse("2026-12-18T12:00:00Z"));

        assertThat(ServicoDeCertificadosNoPainel.caminhoNoZip(certificado))
                .isEqualTo("LGPD o básico parte 1/Ana Aninha Exemplo - ABCD2345EFGH.pdf");
    }

    @Test
    void naoDeixaCaminhoRelativoNemNomeVazioOuLongoDemais() {
        assertThat(ServicoDeCertificadosNoPainel.seguroParaArquivo("../..")).isEqualTo("sem-nome");
        assertThat(ServicoDeCertificadosNoPainel.seguroParaArquivo("   ")).isEqualTo("sem-nome");
        assertThat(ServicoDeCertificadosNoPainel.seguroParaArquivo("A".repeat(300)))
                .hasSize(80);
        assertThat(ServicoDeCertificadosNoPainel.seguroParaArquivo("nome.")).isEqualTo("nome");
    }
}

package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CertificadoTest extends TesteDeIntegracao {

    @Test
    void soEmiteDepoisDeConcluirTodasAsLicoesDoModulo() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");

        pedir(
                post("/api/modulos/{id}/certificado", idDoModuloDaLicao(LICAO_DE_LGPD))
                        .cookie(sessao),
                status().isUnprocessableEntity());
    }

    @Test
    void emiteUmaVezSoEGeraOPdf() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        fazerLicao(sessao, LICAO_DE_LGPD, true);
        long moduloId = idDoModuloDaLicao(LICAO_DE_LGPD);

        JsonNode certificado =
                pedir(post("/api/modulos/{id}/certificado", moduloId).cookie(sessao), status().isOk());
        JsonNode deNovo = pedir(post("/api/modulos/{id}/certificado", moduloId).cookie(sessao), status().isOk());
        String codigo = certificado.get("codigo").asText();

        assertThat(deNovo.get("codigo").asText()).isEqualTo(codigo);
        assertThat(certificado.get("modulo").asText()).isEqualTo("LGPD na prática");

        byte[] pdf = mvc.perform(get("/api/certificados/{codigo}/pdf", codigo).cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(codigo)))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        assertThat(ler("/api/certificados", sessao)).hasSize(1);
        assertThat(ler("/api/eu", sessao)
                        .get("certificados")
                        .get(0)
                        .get("codigo")
                        .asText())
                .isEqualTo(codigo);
    }

    @Test
    void outraPessoaNaoBaixaOPdfMasQualquerUmValidaOCodigo() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        fazerLicao(sessao, LICAO_DE_LGPD, true);
        String codigo = pedir(
                        post("/api/modulos/{id}/certificado", idDoModuloDaLicao(LICAO_DE_LGPD))
                                .cookie(sessao),
                        status().isOk())
                .get("codigo")
                .asText();

        mvc.perform(get("/api/certificados/{codigo}/pdf", codigo).cookie(entrarComo("Carla Demonstração")))
                .andExpect(status().isNotFound());

        String digitado =
                (codigo.substring(0, 4) + "-" + codigo.substring(4, 8) + "-" + codigo.substring(8)).toLowerCase();
        JsonNode validacao = pedir(get("/api/publico/certificados/{codigo}", digitado), status().isOk());
        assertThat(validacao.get("nome").asText()).isEqualTo("Ana Exemplo");
        assertThat(validacao.get("modulo").asText()).isEqualTo("LGPD na prática");
    }

    @Test
    void codigoInexistenteOuMalFormadoNaoEncontraNada() throws Exception {
        mvc.perform(get("/api/publico/certificados/{codigo}", "AAAA-BBBB-CCCC")).andExpect(status().isNotFound());
        mvc.perform(get("/api/publico/certificados/{codigo}", "abc")).andExpect(status().isNotFound());
    }
}

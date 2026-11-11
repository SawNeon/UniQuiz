package br.com.unimed.uniquiz.certificado.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class GeradorDePdfTest {

    private final GeradorDePdf gerador = new GeradorDePdf();

    @Test
    void certificadoTrazNomeModuloDataECodigoDeValidacao() throws IOException {
        byte[] pdf = gerador.gerar(new GeradorDePdf.DadosDoCertificado(
                "Ana Exemplo",
                "LGPD na prática",
                LocalDate.of(2026, 12, 18),
                "ABCD2345EFGH",
                "https://uniquiz.intranet/certificado/ABCD2345EFGH"));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);

            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            assertThat(documento.getPage(0).getMediaBox().getWidth())
                    .isGreaterThan(documento.getPage(0).getMediaBox().getHeight());
            assertThat(documento.getDocumentInformation().getTitle()).contains("LGPD na prática");
            assertThat(texto)
                    .contains("Certificado de conclusão")
                    .contains("Ana Exemplo")
                    .contains("LGPD na prática")
                    .contains("18 de dezembro de 2026")
                    .contains("ABCD-2345-EFGH")
                    .contains("https://uniquiz.intranet/certificado/ABCD2345EFGH");
        }
    }

    @Test
    void nomeComCaractereForaDaFonteNaoQuebraOPdf() throws IOException {
        byte[] pdf = gerador.gerar(new GeradorDePdf.DadosDoCertificado(
                "Łukasz Nguyễn", "IA com responsabilidade", LocalDate.of(2026, 12, 18), "ABCD2345EFGH", "https://x"));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(documento)).contains("ukasz");
        }
    }

    @Test
    void textoMuitoLongoECortado() throws IOException {
        byte[] pdf = gerador.gerar(new GeradorDePdf.DadosDoCertificado(
                "N".repeat(120), "Módulo", LocalDate.of(2026, 12, 18), "ABCD2345EFGH", "https://x"));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(documento)).contains("N".repeat(59) + "…");
        }
    }
}

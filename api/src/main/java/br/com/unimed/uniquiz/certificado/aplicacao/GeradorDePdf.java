package br.com.unimed.uniquiz.certificado.aplicacao;

import br.com.unimed.uniquiz.certificado.dominio.CodigoDeValidacao;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
class GeradorDePdf {

    private static final Color VERDE_UNIMED = new Color(0x00, 0x99, 0x5d);
    private static final Color VERDE_ESCURO = new Color(0x00, 0x4e, 0x4c);
    private static final Color VERDE_CITRICO = new Color(0xb1, 0xd3, 0x4b);
    private static final DateTimeFormatter DATA_POR_EXTENSO =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));

    record DadosDoCertificado(String nome, String modulo, LocalDate emitidoEm, String codigo, String urlDeValidacao) {}

    byte[] gerar(DadosDoCertificado dados) {
        PDRectangle paisagem = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        PDFont negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDDocument documento = new PDDocument();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(paisagem);
            documento.addPage(pagina);
            PDDocumentInformation informacoes = documento.getDocumentInformation();
            informacoes.setTitle("Certificado UniQuiz - " + dados.modulo());
            informacoes.setAuthor("UniQuiz - TI");

            float largura = paisagem.getWidth();
            float altura = paisagem.getHeight();
            try (PDPageContentStream tela = new PDPageContentStream(documento, pagina)) {
                retangulo(tela, VERDE_UNIMED, 0, altura - 70, largura, 70);
                retangulo(tela, VERDE_CITRICO, 0, 0, largura, 18);

                texto(tela, negrito, 22, Color.WHITE, 48, altura - 45, "UniQuiz");
                centralizado(tela, normal, 14, Color.WHITE, largura, altura - 45, "Desafios da TI");

                centralizado(tela, negrito, 34, VERDE_ESCURO, largura, altura - 150, "Certificado de conclusão");
                centralizado(tela, normal, 16, VERDE_ESCURO, largura, altura - 200, "Certificamos que");
                centralizado(tela, negrito, 30, VERDE_UNIMED, largura, altura - 245, ajustar(dados.nome(), 60));
                centralizado(
                        tela, normal, 16, VERDE_ESCURO, largura, altura - 290, "concluiu todas as lições do módulo");
                centralizado(tela, negrito, 24, VERDE_ESCURO, largura, altura - 330, ajustar(dados.modulo(), 70));
                centralizado(
                        tela,
                        normal,
                        14,
                        VERDE_ESCURO,
                        largura,
                        altura - 370,
                        "no UniQuiz, programa de aprendizagem da equipe de TI.");
                centralizado(
                        tela,
                        normal,
                        14,
                        VERDE_ESCURO,
                        largura,
                        altura - 410,
                        DATA_POR_EXTENSO.format(dados.emitidoEm()));

                texto(
                        tela,
                        negrito,
                        12,
                        VERDE_ESCURO,
                        48,
                        60,
                        "Código de validação: " + CodigoDeValidacao.formatado(dados.codigo()));
                texto(tela, normal, 11, VERDE_ESCURO, 48, 42, "Confira em " + dados.urlDeValidacao());
            }
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível gerar o PDF do certificado", erro);
        }
    }

    private static void retangulo(PDPageContentStream tela, Color cor, float x, float y, float largura, float altura)
            throws IOException {
        tela.setNonStrokingColor(cor);
        tela.addRect(x, y, largura, altura);
        tela.fill();
    }

    private static void centralizado(
            PDPageContentStream tela,
            PDFont fonte,
            float tamanho,
            Color cor,
            float larguraDaPagina,
            float y,
            String valor)
            throws IOException {
        float larguraDoTexto = fonte.getStringWidth(valor) / 1000 * tamanho;
        texto(tela, fonte, tamanho, cor, (larguraDaPagina - larguraDoTexto) / 2, y, valor);
    }

    private static void texto(
            PDPageContentStream tela, PDFont fonte, float tamanho, Color cor, float x, float y, String valor)
            throws IOException {
        tela.beginText();
        tela.setFont(fonte, tamanho);
        tela.setNonStrokingColor(cor);
        tela.newLineAtOffset(x, y);
        tela.showText(semCaracteresForaDaFonte(valor));
        tela.endText();
    }

    private static String semCaracteresForaDaFonte(String valor) {
        StringBuilder seguro = new StringBuilder(valor.length());
        for (char caractere : valor.toCharArray()) {
            if (caractere < 256 || caractere == '…') {
                seguro.append(caractere);
            } else {
                String semAcento = Normalizer.normalize(String.valueOf(caractere), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "");
                seguro.append(!semAcento.isEmpty() && semAcento.charAt(0) < 256 ? semAcento.charAt(0) : '?');
            }
        }
        return seguro.toString();
    }

    private static String ajustar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite - 1) + "…";
    }
}

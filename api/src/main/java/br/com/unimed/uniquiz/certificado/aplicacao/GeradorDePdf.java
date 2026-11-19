package br.com.unimed.uniquiz.certificado.aplicacao;

import br.com.unimed.uniquiz.certificado.dominio.CampoDoCertificado;
import br.com.unimed.uniquiz.certificado.dominio.CodigoDeValidacao;
import br.com.unimed.uniquiz.certificado.dominio.LayoutDoCertificado;
import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

@Component
class GeradorDePdf {

    static final PDRectangle PAGINA = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

    private static final Color VERDE_UNIMED = new Color(0x00, 0x99, 0x5d);
    private static final Color VERDE_ESCURO = new Color(0x00, 0x4e, 0x4c);
    private static final Color VERDE_CITRICO = new Color(0xb1, 0xd3, 0x4b);
    private static final float MENOR_REDUCAO_DA_LETRA = 0.6f;
    private static final String RETICENCIAS = "…";
    private static final DateTimeFormatter DATA_POR_EXTENSO =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));

    record DadosDoCertificado(String nome, String modulo, LocalDate emitidoEm, String codigo, String urlDeValidacao) {}

    record Modelo(LayoutDoCertificado layout, byte[] fundoPdf) {

        static Modelo padrao() {
            return new Modelo(LayoutDoCertificado.padrao(), null);
        }

        boolean temArte() {
            return fundoPdf != null;
        }
    }

    byte[] gerar(DadosDoCertificado dados) {
        return gerar(dados, Modelo.padrao());
    }

    byte[] gerar(DadosDoCertificado dados, Modelo modelo) {
        PDFont negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDDocument documento = new PDDocument();
                PDDocument arte = modelo.temArte() ? Loader.loadPDF(modelo.fundoPdf()) : null;
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PAGINA);
            documento.addPage(pagina);
            PDDocumentInformation informacoes = documento.getDocumentInformation();
            informacoes.setTitle("Certificado UniQuiz - " + dados.modulo());
            informacoes.setAuthor("UniQuiz - TI");

            try (PDPageContentStream tela = new PDPageContentStream(documento, pagina)) {
                if (arte != null) {
                    desenharArte(tela, new LayerUtility(documento).importPageAsForm(arte, 0));
                } else {
                    desenharFundoPadrao(tela, negrito, normal);
                }
                Map<CampoDoCertificado, String> valores = Map.of(
                        CampoDoCertificado.NOME, dados.nome(),
                        CampoDoCertificado.MODULO, dados.modulo(),
                        CampoDoCertificado.DATA, DATA_POR_EXTENSO.format(dados.emitidoEm()),
                        CampoDoCertificado.CODIGO, CodigoDeValidacao.formatado(dados.codigo()),
                        CampoDoCertificado.ENDERECO_DE_VALIDACAO, dados.urlDeValidacao());
                for (PosicaoDoCampo posicao : modelo.layout().campos()) {
                    campo(tela, posicao.negrito() ? negrito : normal, posicao, valores.get(posicao.campo()));
                }
            }
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível gerar o PDF do certificado", erro);
        }
    }

    private static void desenharArte(PDPageContentStream tela, PDFormXObject arte) throws IOException {
        PDRectangle caixa = arte.getBBox();
        float escalaX = PAGINA.getWidth() / caixa.getWidth();
        float escalaY = PAGINA.getHeight() / caixa.getHeight();
        tela.saveGraphicsState();
        tela.transform(
                new Matrix(escalaX, 0, 0, escalaY, -caixa.getLowerLeftX() * escalaX, -caixa.getLowerLeftY() * escalaY));
        tela.drawForm(arte);
        tela.restoreGraphicsState();
    }

    private static void desenharFundoPadrao(PDPageContentStream tela, PDFont negrito, PDFont normal)
            throws IOException {
        float largura = PAGINA.getWidth();
        float altura = PAGINA.getHeight();
        retangulo(tela, VERDE_UNIMED, 0, altura - 70, largura, 70);
        retangulo(tela, VERDE_CITRICO, 0, 0, largura, 18);
        texto(tela, negrito, 22, Color.WHITE, 48, altura - 45, "UniQuiz");
        centralizado(tela, normal, 14, Color.WHITE, altura - 45, "Desafios da TI");
        centralizado(tela, negrito, 34, VERDE_ESCURO, altura - 150, "Certificado de conclusão");
        centralizado(tela, normal, 16, VERDE_ESCURO, altura - 200, "Certificamos que");
        centralizado(tela, normal, 16, VERDE_ESCURO, altura - 290, "concluiu todas as lições do módulo");
        centralizado(
                tela, normal, 14, VERDE_ESCURO, altura - 370, "no UniQuiz, programa de aprendizagem da equipe de TI.");
    }

    private static void campo(PDPageContentStream tela, PDFont fonte, PosicaoDoCampo posicao, String valor)
            throws IOException {
        if (!posicao.visivel() || valor == null) {
            return;
        }
        float limite = (float) (posicao.larguraMaxima() / 100 * PAGINA.getWidth());
        float tamanho = (float) posicao.tamanho();
        float menor = tamanho * MENOR_REDUCAO_DA_LETRA;
        String texto = semCaracteresForaDaFonte(posicao.prefixoOuVazio() + valor);
        while (larguraDe(fonte, texto, tamanho) > limite && tamanho > menor) {
            tamanho = Math.max(menor, tamanho - 0.5f);
        }
        texto = cortado(fonte, texto, tamanho, limite);

        float larguraDoTexto = larguraDe(fonte, texto, tamanho);
        float ancora = (float) (posicao.x() / 100 * PAGINA.getWidth());
        float x =
                switch (posicao.alinhamento()) {
                    case ESQUERDA -> ancora;
                    case CENTRO -> ancora - larguraDoTexto / 2;
                    case DIREITA -> ancora - larguraDoTexto;
                };
        float y = (float) ((100 - posicao.y()) / 100 * PAGINA.getHeight());
        texto(tela, fonte, tamanho, Color.decode(posicao.cor()), x, y, texto);
    }

    private static String cortado(PDFont fonte, String texto, float tamanho, float limite) throws IOException {
        if (larguraDe(fonte, texto, tamanho) <= limite) {
            return texto;
        }
        int letras = texto.length();
        while (letras > 1 && larguraDe(fonte, texto.substring(0, letras) + RETICENCIAS, tamanho) > limite) {
            letras--;
        }
        return texto.substring(0, letras).strip() + RETICENCIAS;
    }

    private static float larguraDe(PDFont fonte, String texto, float tamanho) throws IOException {
        return fonte.getStringWidth(texto) / 1000 * tamanho;
    }

    private static void retangulo(PDPageContentStream tela, Color cor, float x, float y, float largura, float altura)
            throws IOException {
        tela.setNonStrokingColor(cor);
        tela.addRect(x, y, largura, altura);
        tela.fill();
    }

    private static void centralizado(
            PDPageContentStream tela, PDFont fonte, float tamanho, Color cor, float y, String valor)
            throws IOException {
        String seguro = semCaracteresForaDaFonte(valor);
        texto(tela, fonte, tamanho, cor, (PAGINA.getWidth() - larguraDe(fonte, seguro, tamanho)) / 2, y, seguro);
    }

    private static void texto(
            PDPageContentStream tela, PDFont fonte, float tamanho, Color cor, float x, float y, String valor)
            throws IOException {
        tela.beginText();
        tela.setFont(fonte, tamanho);
        tela.setNonStrokingColor(cor);
        tela.newLineAtOffset(x, y);
        tela.showText(valor);
        tela.endText();
    }

    static String semCaracteresForaDaFonte(String valor) {
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
}

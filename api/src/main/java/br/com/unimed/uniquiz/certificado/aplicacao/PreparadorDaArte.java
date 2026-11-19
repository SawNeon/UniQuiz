package br.com.unimed.uniquiz.certificado.aplicacao;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

@Component
class PreparadorDaArte {

    static final int TAMANHO_MAXIMO = 10 * 1024 * 1024;

    private static final long PIXELS_MAXIMOS = 60_000_000L;
    private static final int LARGURA_BOA_PARA_IMPRESSAO = 1754;
    private static final double PROPORCAO_DO_A4 = 297.0 / 210.0;
    private static final double TOLERANCIA_DA_PROPORCAO = 0.03;
    private static final float DPI_DA_PREVIA = 110;
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PDF = {'%', 'P', 'D', 'F', '-'};

    record ArtePreparada(byte[] fundoPdf, byte[] previa, List<String> avisos) {}

    private record Dimensoes(double largura, double altura) {

        double proporcao() {
            return largura / altura;
        }
    }

    ArtePreparada preparar(byte[] arquivo) {
        if (arquivo == null || arquivo.length == 0) {
            throw new ErroDeNegocio("Envie o arquivo da arte do certificado.");
        }
        if (arquivo.length > TAMANHO_MAXIMO) {
            throw new ErroDeNegocio("A arte pode ter no máximo 10 MB.");
        }
        List<String> avisos = new ArrayList<>();
        byte[] fundo;
        if (comecaCom(arquivo, ASSINATURA_PDF)) {
            fundo = deUmPdf(arquivo, avisos);
        } else if (comecaCom(arquivo, ASSINATURA_PNG) || comecaCom(arquivo, ASSINATURA_JPEG)) {
            fundo = deUmaImagem(arquivo, avisos);
        } else {
            throw new ErroDeNegocio("Envie a arte em PNG, JPG ou PDF.");
        }
        return new ArtePreparada(fundo, previa(fundo), List.copyOf(avisos));
    }

    byte[] previa(byte[] fundoPdf) {
        try (PDDocument documento = Loader.loadPDF(fundoPdf);
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            BufferedImage imagem = new PDFRenderer(documento).renderImageWithDPI(0, DPI_DA_PREVIA, ImageType.RGB);
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível gerar a prévia da arte", erro);
        }
    }

    private byte[] deUmaImagem(byte[] arquivo, List<String> avisos) {
        Dimensoes dimensoes = dimensoesDaImagem(arquivo);
        if (dimensoes.largura() * dimensoes.altura() > PIXELS_MAXIMOS) {
            throw new ErroDeNegocio("A imagem é grande demais. Use no máximo 7016 × 4961 pixels (A4 a 600 dpi).");
        }
        avisarProporcao(dimensoes, avisos);
        if (dimensoes.largura() < LARGURA_BOA_PARA_IMPRESSAO) {
            avisos.add("A imagem tem %.0f pixels de largura e pode sair borrada na impressão. O ideal é 3508 × 2480."
                    .formatted(dimensoes.largura()));
        }
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(GeradorDePdf.PAGINA);
            documento.addPage(pagina);
            PDImageXObject imagem = PDImageXObject.createFromByteArray(documento, arquivo, "arte");
            try (PDPageContentStream tela = new PDPageContentStream(documento, pagina)) {
                tela.drawImage(imagem, 0, 0, GeradorDePdf.PAGINA.getWidth(), GeradorDePdf.PAGINA.getHeight());
            }
            return salvar(documento);
        } catch (IOException | IllegalArgumentException erro) {
            throw new ErroDeNegocio("Não foi possível ler a imagem. Confira se o arquivo não está corrompido.");
        }
    }

    private byte[] deUmPdf(byte[] arquivo, List<String> avisos) {
        try (PDDocument original = Loader.loadPDF(arquivo);
                PDDocument documento = new PDDocument()) {
            if (original.getNumberOfPages() == 0) {
                throw new ErroDeNegocio("O PDF da arte não tem nenhuma página.");
            }
            if (original.getNumberOfPages() > 1) {
                avisos.add("O PDF tem mais de uma página. Só a primeira será usada.");
            }
            PDPage primeira = original.getPage(0);
            int rotacao = Math.floorMod(primeira.getRotation(), 360);
            PDPage pagina = new PDPage(GeradorDePdf.PAGINA);
            documento.addPage(pagina);
            PDFormXObject arte = new LayerUtility(documento).importPageAsForm(original, 0);
            arte.setMatrix(new AffineTransform());
            PDRectangle caixa = arte.getBBox();
            boolean deitada = rotacao == 90 || rotacao == 270;
            avisarProporcao(
                    deitada
                            ? new Dimensoes(caixa.getHeight(), caixa.getWidth())
                            : new Dimensoes(caixa.getWidth(), caixa.getHeight()),
                    avisos);
            try (PDPageContentStream tela = new PDPageContentStream(documento, pagina)) {
                tela.transform(encaixe(caixa, rotacao));
                tela.drawForm(arte);
            }
            return salvar(documento);
        } catch (ErroDeNegocio erro) {
            throw erro;
        } catch (IOException | RuntimeException erro) {
            throw new ErroDeNegocio(
                    "Não foi possível ler o PDF da arte. Exporte de novo, sem senha, e tente outra vez.");
        }
    }

    private static Matrix encaixe(PDRectangle caixa, int rotacao) {
        float largura = GeradorDePdf.PAGINA.getWidth();
        float altura = GeradorDePdf.PAGINA.getHeight();
        float x0 = caixa.getLowerLeftX();
        float y0 = caixa.getLowerLeftY();
        float w = caixa.getWidth();
        float h = caixa.getHeight();
        return switch (rotacao) {
            case 90 -> new Matrix(0, -altura / w, largura / h, 0, -y0 * largura / h, altura / w * (w + x0));
            case 180 -> new Matrix(-largura / w, 0, 0, -altura / h, largura / w * (w + x0), altura / h * (h + y0));
            case 270 -> new Matrix(0, altura / w, -largura / h, 0, largura / h * (h + y0), -x0 * altura / w);
            default -> new Matrix(largura / w, 0, 0, altura / h, -x0 * largura / w, -y0 * altura / h);
        };
    }

    private static void avisarProporcao(Dimensoes dimensoes, List<String> avisos) {
        if (dimensoes.proporcao() < 1) {
            avisos.add("A arte está em pé. O certificado é em A4 deitado (paisagem), então ela foi esticada.");
        } else if (Math.abs(dimensoes.proporcao() - PROPORCAO_DO_A4) / PROPORCAO_DO_A4 > TOLERANCIA_DA_PROPORCAO) {
            avisos.add("A arte não tem a proporção do A4 (297 × 210 mm) e foi esticada para ocupar a página.");
        }
    }

    private static Dimensoes dimensoesDaImagem(byte[] arquivo) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(arquivo))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw new ErroDeNegocio("Não foi possível ler a imagem. Envie em PNG, JPG ou PDF.");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                return new Dimensoes(leitor.getWidth(0), leitor.getHeight(0));
            } finally {
                leitor.dispose();
            }
        } catch (IOException erro) {
            throw new ErroDeNegocio("Não foi possível ler a imagem. Confira se o arquivo não está corrompido.");
        }
    }

    private static byte[] salvar(PDDocument documento) throws IOException {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private static boolean comecaCom(byte[] arquivo, byte[] assinatura) {
        return arquivo.length >= assinatura.length
                && Arrays.equals(Arrays.copyOf(arquivo, assinatura.length), assinatura);
    }
}

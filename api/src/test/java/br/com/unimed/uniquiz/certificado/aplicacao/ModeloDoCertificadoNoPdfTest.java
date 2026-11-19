package br.com.unimed.uniquiz.certificado.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.certificado.dominio.CampoDoCertificado;
import br.com.unimed.uniquiz.certificado.dominio.LayoutDoCertificado;
import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo;
import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo.Alinhamento;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class ModeloDoCertificadoNoPdfTest {

    private static final GeradorDePdf.DadosDoCertificado DADOS = new GeradorDePdf.DadosDoCertificado(
            "Ana Exemplo",
            "LGPD na prática",
            LocalDate.of(2026, 12, 18),
            "ABCD2345EFGH",
            "https://uniquiz.intranet/certificado/ABCD2345EFGH");

    private final GeradorDePdf gerador = new GeradorDePdf();
    private final PreparadorDaArte preparador = new PreparadorDaArte();

    @Test
    void comArteDeFundoNaoDesenhaOFundoPadraoMasMantemOsCampos() throws IOException {
        PreparadorDaArte.ArtePreparada arte = preparador.preparar(imagem(3508, 2480, "png"));

        byte[] pdf = gerador.gerar(DADOS, new GeradorDePdf.Modelo(LayoutDoCertificado.padrao(), arte.fundoPdf()));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            assertThat(documento.getPage(0).getResources().getXObjectNames()).isNotEmpty();
            assertThat(texto)
                    .contains("Ana Exemplo")
                    .contains("LGPD na prática")
                    .contains("Código de validação: ABCD-2345-EFGH")
                    .doesNotContain("Certificamos que");
        }
        assertThat(arte.avisos()).isEmpty();
        assertThat(arte.previa()).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void campoEscondidoNaoApareceEPrefixoVemAntesDoValor() throws IOException {
        List<PosicaoDoCampo> campos =
                new ArrayList<>(LayoutDoCertificado.padrao().campos());
        campos.replaceAll(posicao -> switch (posicao.campo()) {
            case ENDERECO_DE_VALIDACAO -> comVisibilidade(posicao, false);
            case DATA -> comPrefixo(posicao, "Varginha, ");
            default -> posicao;
        });

        byte[] pdf = gerador.gerar(DADOS, new GeradorDePdf.Modelo(new LayoutDoCertificado(campos), null));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(documento))
                    .contains("Varginha, 18 de dezembro de 2026")
                    .doesNotContain("uniquiz.intranet");
        }
    }

    @Test
    void alinhamentoADireitaTerminaNaPosicaoEscolhida() throws IOException {
        List<PosicaoDoCampo> campos =
                new ArrayList<>(LayoutDoCertificado.padrao().campos());
        campos.replaceAll(posicao -> posicao.campo() == CampoDoCertificado.NOME
                ? new PosicaoDoCampo(
                        CampoDoCertificado.NOME, true, 90, 20, 20, "#112233", Alinhamento.DIREITA, false, 80, "")
                : posicao);

        byte[] pdf = gerador.gerar(DADOS, new GeradorDePdf.Modelo(new LayoutDoCertificado(campos), null));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            List<TextPosition> letras = letrasDe(documento, "Ana Exemplo");
            TextPosition ultima = letras.get(letras.size() - 1);
            float fimDoNome = ultima.getXDirAdj() + ultima.getWidthDirAdj();
            float larguraDaPagina = documento.getPage(0).getMediaBox().getWidth();
            assertThat(fimDoNome / larguraDaPagina * 100).isBetween(89.5f, 90.5f);
            assertThat(letras.get(0).getYDirAdj()
                            / documento.getPage(0).getMediaBox().getHeight()
                            * 100)
                    .isBetween(19f, 21f);
        }
    }

    @Test
    void jpgPequenoEEmPeGeraAvisos() {
        PreparadorDaArte.ArtePreparada arte = preparador.preparar(imagem(800, 1200, "jpg"));

        assertThat(arte.avisos())
                .anySatisfy(aviso -> assertThat(aviso).contains("em pé"))
                .anySatisfy(aviso -> assertThat(aviso).contains("borrada"));
    }

    @Test
    void pdfComMaisDeUmaPaginaUsaSoAPrimeira() throws IOException {
        PreparadorDaArte.ArtePreparada arte = preparador.preparar(pdf(2, 0));

        assertThat(arte.avisos()).anySatisfy(aviso -> assertThat(aviso).contains("primeira"));
        try (PDDocument fundo = Loader.loadPDF(arte.fundoPdf())) {
            assertThat(fundo.getNumberOfPages()).isEqualTo(1);
            assertThat(fundo.getPage(0).getMediaBox().getWidth()).isEqualTo(GeradorDePdf.PAGINA.getWidth());
            assertThat(new PDFTextStripper().getText(fundo)).contains("Arte da comunicação");
        }
    }

    @Test
    void pdfEmPeComRotacaoViraPaisagemSemAviso() throws IOException {
        PreparadorDaArte.ArtePreparada arte = preparador.preparar(pdf(1, 90));

        assertThat(arte.avisos()).isEmpty();
        try (PDDocument fundo = Loader.loadPDF(arte.fundoPdf())) {
            assertThat(fundo.getPage(0).getRotation()).isZero();
            assertThat(new PDFTextStripper().getText(fundo).replaceAll("\\s", ""))
                    .contains("Artedacomunicação");
        }
    }

    @Test
    void recusaArquivoQueNaoEImagemNemPdf() {
        assertThatThrownBy(() -> preparador.preparar("nome,setor".getBytes()))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("PNG, JPG ou PDF");
        assertThatThrownBy(() -> preparador.preparar(new byte[0])).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> preparador.preparar(new byte[] {'%', 'P', 'D', 'F', '-', '1'}))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void recusaArteMaiorQueOLimite() {
        byte[] grande = new byte[PreparadorDaArte.TAMANHO_MAXIMO + 1];
        grande[0] = (byte) 0x89;

        assertThatThrownBy(() -> preparador.preparar(grande)).hasMessageContaining("10 MB");
    }

    private static PosicaoDoCampo comVisibilidade(PosicaoDoCampo posicao, boolean visivel) {
        return new PosicaoDoCampo(
                posicao.campo(),
                visivel,
                posicao.x(),
                posicao.y(),
                posicao.tamanho(),
                posicao.cor(),
                posicao.alinhamento(),
                posicao.negrito(),
                posicao.larguraMaxima(),
                posicao.prefixo());
    }

    private static PosicaoDoCampo comPrefixo(PosicaoDoCampo posicao, String prefixo) {
        return new PosicaoDoCampo(
                posicao.campo(),
                posicao.visivel(),
                posicao.x(),
                posicao.y(),
                posicao.tamanho(),
                posicao.cor(),
                posicao.alinhamento(),
                posicao.negrito(),
                posicao.larguraMaxima(),
                prefixo);
    }

    private static List<TextPosition> letrasDe(PDDocument documento, String procurado) throws IOException {
        List<TextPosition> letras = new ArrayList<>();
        PDFTextStripper leitor = new PDFTextStripper() {
            @Override
            protected void writeString(String texto, List<TextPosition> posicoes) throws IOException {
                if (texto.equals(procurado)) {
                    letras.addAll(posicoes);
                }
                super.writeString(texto, posicoes);
            }
        };
        leitor.getText(documento);
        assertThat(letras).isNotEmpty();
        return letras;
    }

    static byte[] imagem(int largura, int altura, String formato) {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D desenho = imagem.createGraphics();
        desenho.setColor(new Color(0x00, 0x99, 0x5d));
        desenho.fillRect(0, 0, largura, altura / 6);
        desenho.setColor(Color.WHITE);
        desenho.fillRect(0, altura / 6, largura, altura);
        desenho.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, formato, saida);
            return saida.toByteArray();
        } catch (IOException erro) {
            throw new IllegalStateException(erro);
        }
    }

    static byte[] pdf(int paginas, int rotacao) throws IOException {
        try (PDDocument documento = new PDDocument();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            for (int numero = 0; numero < paginas; numero++) {
                PDPage pagina = new PDPage(rotacao == 0 ? GeradorDePdf.PAGINA : PDRectangle.A4);
                pagina.setRotation(rotacao);
                documento.addPage(pagina);
                try (PDPageContentStream tela = new PDPageContentStream(documento, pagina)) {
                    tela.beginText();
                    tela.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 20);
                    tela.newLineAtOffset(60, 100);
                    tela.showText("Arte da comunicação");
                    tela.endText();
                }
            }
            documento.save(saida);
            return saida.toByteArray();
        }
    }
}

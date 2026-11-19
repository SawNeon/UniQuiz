package br.com.unimed.uniquiz.certificado.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo.Alinhamento;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class LayoutDoCertificadoTest {

    @Test
    void padraoTemTodosOsCamposVisiveisEmOrdem() {
        LayoutDoCertificado padrao = LayoutDoCertificado.padrao();

        assertThat(padrao.campos()).extracting(PosicaoDoCampo::campo).containsExactly(CampoDoCertificado.values());
        assertThat(padrao.campos()).allMatch(PosicaoDoCampo::visivel);
        assertThat(padrao.posicaoDo(CampoDoCertificado.CODIGO).prefixo()).isEqualTo("Código de validação: ");
    }

    @Test
    void ordenaOsCamposPadronizaACorEAceitaPrefixoVazio() {
        List<PosicaoDoCampo> invertidos =
                new ArrayList<>(LayoutDoCertificado.padrao().campos());
        Collections.reverse(invertidos);
        invertidos.replaceAll(posicao -> com(posicao, "#abcdef", null));

        LayoutDoCertificado layout = new LayoutDoCertificado(invertidos);

        assertThat(layout.campos().get(0).campo()).isEqualTo(CampoDoCertificado.NOME);
        assertThat(layout.campos()).allMatch(posicao -> posicao.cor().equals("#ABCDEF"));
        assertThat(layout.campos()).allMatch(posicao -> posicao.prefixo().isEmpty());
    }

    @Test
    void recusaCampoRepetidoOuFaltando() {
        List<PosicaoDoCampo> campos =
                new ArrayList<>(LayoutDoCertificado.padrao().campos());
        campos.set(1, campos.get(0));
        assertThatThrownBy(() -> new LayoutDoCertificado(campos))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("uma vez");

        assertThatThrownBy(() -> new LayoutDoCertificado(
                        LayoutDoCertificado.padrao().campos().subList(0, 3)))
                .hasMessageContaining("todos os campos");
        assertThatThrownBy(() -> new LayoutDoCertificado(List.of())).isInstanceOf(ErroDeNegocio.class);
    }

    @Test
    void recusaValoresForaDosLimites() {
        assertThatThrownBy(() -> alterarNome(posicao -> new PosicaoDoCampo(
                        posicao.campo(), true, 101, 10, 12, "#000000", Alinhamento.CENTRO, false, 50, "")))
                .hasMessageContaining("dentro da página");
        assertThatThrownBy(() -> alterarNome(posicao -> new PosicaoDoCampo(
                        posicao.campo(), true, 10, 10, 200, "#000000", Alinhamento.CENTRO, false, 50, "")))
                .hasMessageContaining("tamanho da letra");
        assertThatThrownBy(() -> alterarNome(posicao -> com(posicao, "verde", "")))
                .hasMessageContaining("#RRGGBB");
        assertThatThrownBy(() -> alterarNome(posicao ->
                        new PosicaoDoCampo(posicao.campo(), true, 10, 10, 12, "#000000", null, false, 50, "")))
                .hasMessageContaining("alinhamento");
        assertThatThrownBy(() -> alterarNome(posicao -> new PosicaoDoCampo(
                        posicao.campo(), true, 10, 10, 12, "#000000", Alinhamento.CENTRO, false, 2, "")))
                .hasMessageContaining("largura máxima");
        assertThatThrownBy(() -> alterarNome(posicao -> com(posicao, "#000000", "x".repeat(81))))
                .hasMessageContaining("80 letras");
        assertThatThrownBy(() -> alterarNome(posicao -> com(posicao, "#000000", "linha\nquebrada")))
                .hasMessageContaining("quebra de linha");
        assertThatThrownBy(() -> alterarNome(posicao -> new PosicaoDoCampo(
                        posicao.campo(), true, Double.NaN, 10, 12, "#000000", Alinhamento.CENTRO, false, 50, "")))
                .hasMessageContaining("dentro da página");
    }

    private static LayoutDoCertificado alterarNome(UnaryOperator<PosicaoDoCampo> mudanca) {
        List<PosicaoDoCampo> campos =
                new ArrayList<>(LayoutDoCertificado.padrao().campos());
        campos.replaceAll(posicao -> posicao.campo() == CampoDoCertificado.NOME ? mudanca.apply(posicao) : posicao);
        return new LayoutDoCertificado(campos);
    }

    private static PosicaoDoCampo com(PosicaoDoCampo posicao, String cor, String prefixo) {
        return new PosicaoDoCampo(
                posicao.campo(),
                posicao.visivel(),
                posicao.x(),
                posicao.y(),
                posicao.tamanho(),
                cor,
                posicao.alinhamento(),
                posicao.negrito(),
                posicao.larguraMaxima(),
                prefixo);
    }
}

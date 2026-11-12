package br.com.unimed.uniquiz.conteudo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import org.junit.jupiter.api.Test;

class QuantidadesPorNivelTest {

    @Test
    void somaOTotalEDizQuantosDeCadaNivel() {
        QuantidadesPorNivel quantidades = new QuantidadesPorNivel(5, 4, 3);

        assertThat(quantidades.total()).isEqualTo(12);
        assertThat(quantidades.de(Dificuldade.FACIL)).isEqualTo(5);
        assertThat(quantidades.de(Dificuldade.MEDIO)).isEqualTo(4);
        assertThat(quantidades.de(Dificuldade.DIFICIL)).isEqualTo(3);
    }

    @Test
    void aceitaDeZeroAoMaximoPorNivel() {
        assertThat(new QuantidadesPorNivel(0, 0, 0).total()).isZero();
        assertThat(new QuantidadesPorNivel(30, 30, 30).total()).isEqualTo(90);
    }

    @Test
    void recusaNegativoEAcimaDoMaximo() {
        assertThatThrownBy(() -> new QuantidadesPorNivel(-1, 0, 0))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("0 a 30");
        assertThatThrownBy(() -> new QuantidadesPorNivel(0, 31, 0)).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> new QuantidadesPorNivel(0, 0, 99)).isInstanceOf(ErroDeNegocio.class);
    }
}

package br.com.unimed.uniquiz.jogo.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SequenciaDeDiasTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 9);

    @Test
    void diaSeguidoAumentaASequencia() {
        SequenciaDeDias sequencia = SequenciaDeDias.primeiroDia(SEGUNDA).registrar(SEGUNDA.plusDays(1));

        assertThat(sequencia.diasAtuais()).isEqualTo(2);
        assertThat(sequencia.maiorSequencia()).isEqualTo(2);
    }

    @Test
    void mesmoDiaNaoConta() {
        SequenciaDeDias sequencia = SequenciaDeDias.primeiroDia(SEGUNDA).registrar(SEGUNDA);

        assertThat(sequencia.diasAtuais()).isEqualTo(1);
    }

    @Test
    void fimDeSemanaNaoQuebraASequencia() {
        LocalDate sexta = SEGUNDA.plusDays(4);
        LocalDate proximaSegunda = SEGUNDA.plusDays(7);

        SequenciaDeDias sequencia = new SequenciaDeDias(5, 5, sexta).registrar(proximaSegunda);

        assertThat(sequencia.diasAtuais()).isEqualTo(6);
    }

    @Test
    void faltarUmDiaUtilRecomecaMasGuardaOMaior() {
        SequenciaDeDias sequencia = new SequenciaDeDias(4, 4, SEGUNDA).registrar(SEGUNDA.plusDays(2));

        assertThat(sequencia.diasAtuais()).isEqualTo(1);
        assertThat(sequencia.maiorSequencia()).isEqualTo(4);
    }

    @Test
    void diasVigentesZeraQuandoASequenciaJaQuebrou() {
        SequenciaDeDias sequencia = new SequenciaDeDias(3, 3, SEGUNDA);

        assertThat(sequencia.diasVigentes(SEGUNDA.plusDays(1))).isEqualTo(3);
        assertThat(sequencia.diasVigentes(SEGUNDA.plusDays(2))).isZero();
    }
}

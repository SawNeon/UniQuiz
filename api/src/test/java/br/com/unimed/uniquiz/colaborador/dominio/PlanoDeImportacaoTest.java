package br.com.unimed.uniquiz.colaborador.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlanoDeImportacaoTest {

    private static final List<ColaboradorExistente> EXISTENTES = List.of(
            new ColaboradorExistente(1L, "Unimed Exemplo", "Faturamento", "Maria de Souza", true),
            new ColaboradorExistente(2L, "Unimed Exemplo", "Faturamento", "Pedro Antigo", true),
            new ColaboradorExistente(3L, "Unimed Exemplo", "TI", "Lia Volta", false));

    @Test
    void separaNovosMantidosReativadosEInativados() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "MARIA DE SOUZA", "faturamento", "Unimed Exemplo", "123"),
                        new LinhaDaPlanilha(3, "Lia Volta", "TI", "Unimed Exemplo", "123"),
                        new LinhaDaPlanilha(4, "João Novo", "TI", "Unimed Exemplo", "123")),
                EXISTENTES);

        assertThat(plano.mantidos()).isEqualTo(1);
        assertThat(plano.reativados()).containsExactly(3L);
        assertThat(plano.novos()).extracting(LinhaDaPlanilha::nome).containsExactly("João Novo");
        assertThat(plano.inativados()).containsExactly(2L);
        assertThat(plano.podeConfirmar()).isTrue();
    }

    @Test
    void comparaSemAcentoEspacosOuMaiusculas() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(new LinhaDaPlanilha(2, "  maria  de souza ", "FATURAMENTO", "unimed exemplo", "123")),
                List.of(EXISTENTES.getFirst()));

        assertThat(plano.novos()).isEmpty();
        assertThat(plano.mantidos()).isEqualTo(1);
    }

    @Test
    void avisaHomonimosNoMesmoSetorEImpedeAConfirmacao() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "João Silva", "Fiscal", "Unimed Exemplo", "123"),
                        new LinhaDaPlanilha(5, "Joao Silva", "Fiscal", "Unimed Exemplo", "123")),
                List.of());

        assertThat(plano.homonimos()).hasSize(1).first().asString().contains("2 e 5");
        assertThat(plano.podeConfirmar()).isFalse();
    }

    @Test
    void linhaIncompletaViraProblemaELinhaVaziaEIgnorada() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "Ana", "", "Unimed Exemplo", "123"),
                        new LinhaDaPlanilha(3, "", "", "", ""),
                        new LinhaDaPlanilha(4, "Rui", "TI", "Unimed Exemplo", "123")),
                List.of());

        assertThat(plano.problemas()).containsExactly("Linha 2: preencha nome, setor e empresa.");
        assertThat(plano.novos()).hasSize(1);
        assertThat(plano.podeConfirmar()).isFalse();
    }

    @Test
    void pinForaDoFormatoViraProblemaEOZeroQueOExcelApagaVolta() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "Ana", "TI", "Unimed Exemplo", ""),
                        new LinhaDaPlanilha(3, "Rui", "TI", "Unimed Exemplo", "12a"),
                        new LinhaDaPlanilha(4, "Lia", "TI", "Unimed Exemplo", "1234"),
                        new LinhaDaPlanilha(5, "Bia", "TI", "Unimed Exemplo", "7")),
                List.of());

        assertThat(plano.problemas())
                .containsExactly(
                        "Linha 2: o PIN de Ana precisa ter 3 números.",
                        "Linha 3: o PIN de Rui precisa ter 3 números.",
                        "Linha 4: o PIN de Lia precisa ter 3 números.");
        assertThat(plano.novos()).extracting(LinhaDaPlanilha::pin).containsExactly("007");
        assertThat(plano.podeConfirmar()).isFalse();
    }

    @Test
    void guardaOPinDaPlanilhaParaQuemJaExiste() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "Maria de Souza", "Faturamento", "Unimed Exemplo", "111"),
                        new LinhaDaPlanilha(3, "Lia Volta", "TI", "Unimed Exemplo", "222"),
                        new LinhaDaPlanilha(4, "João Novo", "TI", "Unimed Exemplo", "333")),
                EXISTENTES);

        assertThat(plano.pinsDosExistentes()).containsExactlyInAnyOrderEntriesOf(Map.of(1L, "111", 3L, "222"));
        assertThat(plano.novos()).extracting(LinhaDaPlanilha::pin).containsExactly("333");
    }

    @Test
    void planilhaSemNinguemNaoInativaTodoMundo() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(List.of(), EXISTENTES);

        assertThat(plano.podeConfirmar()).isFalse();
    }
}

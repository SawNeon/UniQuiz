package br.com.unimed.uniquiz.colaborador.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
                        new LinhaDaPlanilha(2, "MARIA DE SOUZA", "faturamento", "Unimed Exemplo"),
                        new LinhaDaPlanilha(3, "Lia Volta", "TI", "Unimed Exemplo"),
                        new LinhaDaPlanilha(4, "João Novo", "TI", "Unimed Exemplo")),
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
                List.of(new LinhaDaPlanilha(2, "  maria  de souza ", "FATURAMENTO", "unimed exemplo")),
                List.of(EXISTENTES.getFirst()));

        assertThat(plano.novos()).isEmpty();
        assertThat(plano.mantidos()).isEqualTo(1);
    }

    @Test
    void avisaHomonimosNoMesmoSetorEImpedeAConfirmacao() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "João Silva", "Fiscal", "Unimed Exemplo"),
                        new LinhaDaPlanilha(5, "Joao Silva", "Fiscal", "Unimed Exemplo")),
                List.of());

        assertThat(plano.homonimos()).hasSize(1).first().asString().contains("2 e 5");
        assertThat(plano.podeConfirmar()).isFalse();
    }

    @Test
    void linhaIncompletaViraProblemaELinhaVaziaEIgnorada() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(
                List.of(
                        new LinhaDaPlanilha(2, "Ana", "", "Unimed Exemplo"),
                        new LinhaDaPlanilha(3, "", "", ""),
                        new LinhaDaPlanilha(4, "Rui", "TI", "Unimed Exemplo")),
                List.of());

        assertThat(plano.problemas()).containsExactly("Linha 2: preencha nome, setor e empresa.");
        assertThat(plano.novos()).hasSize(1);
        assertThat(plano.podeConfirmar()).isFalse();
    }

    @Test
    void planilhaSemNinguemNaoInativaTodoMundo() {
        PlanoDeImportacao plano = PlanoDeImportacao.montar(List.of(), EXISTENTES);

        assertThat(plano.podeConfirmar()).isFalse();
    }
}

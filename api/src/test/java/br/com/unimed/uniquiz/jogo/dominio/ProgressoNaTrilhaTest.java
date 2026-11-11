package br.com.unimed.uniquiz.jogo.dominio;

import static br.com.unimed.uniquiz.jogo.dominio.ProgressoNaTrilha.SituacaoDaLicao.BLOQUEADA;
import static br.com.unimed.uniquiz.jogo.dominio.ProgressoNaTrilha.SituacaoDaLicao.CONCLUIDA;
import static br.com.unimed.uniquiz.jogo.dominio.ProgressoNaTrilha.SituacaoDaLicao.DISPONIVEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgressoNaTrilhaTest {

    @Test
    void liberaSoAProximaLicaoNaoConcluida() {
        assertThat(ProgressoNaTrilha.situacoes(List.of(1L, 2L, 3L, 4L), Set.of(1L)))
                .containsExactly(
                        entry(1L, CONCLUIDA), entry(2L, DISPONIVEL), entry(3L, BLOQUEADA), entry(4L, BLOQUEADA));
    }

    @Test
    void licaoNovaNoMeioDaTrilhaFicaDisponivelSemPerderAsConcluidas() {
        assertThat(ProgressoNaTrilha.situacoes(List.of(1L, 9L, 2L), Set.of(1L, 2L)))
                .containsEntry(9L, DISPONIVEL)
                .containsEntry(2L, CONCLUIDA);
    }

    @Test
    void semNadaConcluidoAPrimeiraFicaDisponivel() {
        assertThat(ProgressoNaTrilha.situacoes(List.of(5L, 6L), Set.of()))
                .containsEntry(5L, DISPONIVEL)
                .containsEntry(6L, BLOQUEADA);
    }
}

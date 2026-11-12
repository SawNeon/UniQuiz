package br.com.unimed.uniquiz.conteudo.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SorteioDeDesafiosTest {

    private record Item(String nome, Dificuldade nivel) {}

    private static final List<Item> BANCO = List.of(
            new Item("f1", Dificuldade.FACIL),
            new Item("f2", Dificuldade.FACIL),
            new Item("f3", Dificuldade.FACIL),
            new Item("f4", Dificuldade.FACIL),
            new Item("f5", Dificuldade.FACIL),
            new Item("f6", Dificuldade.FACIL),
            new Item("m1", Dificuldade.MEDIO),
            new Item("m2", Dificuldade.MEDIO),
            new Item("m3", Dificuldade.MEDIO),
            new Item("m4", Dificuldade.MEDIO),
            new Item("d1", Dificuldade.DIFICIL),
            new Item("d2", Dificuldade.DIFICIL),
            new Item("d3", Dificuldade.DIFICIL));

    private static List<Item> sortear(QuantidadesPorNivel quantidades, long semente) {
        return SorteioDeDesafios.sortear(BANCO, Item::nivel, quantidades, new Random(semente));
    }

    @Test
    void trazExatamenteAQuantidadeDeCadaNivel() {
        List<Item> sorteados = sortear(new QuantidadesPorNivel(5, 3, 2), 1);

        assertThat(sorteados).hasSize(10);
        assertThat(sorteados.stream().filter(item -> item.nivel() == Dificuldade.FACIL))
                .hasSize(5);
        assertThat(sorteados.stream().filter(item -> item.nivel() == Dificuldade.MEDIO))
                .hasSize(3);
        assertThat(sorteados.stream().filter(item -> item.nivel() == Dificuldade.DIFICIL))
                .hasSize(2);
    }

    @Test
    void naoRepeteDesafioNemInventaDesafioForaDoBanco() {
        for (long semente = 0; semente < 50; semente++) {
            List<Item> sorteados = sortear(new QuantidadesPorNivel(6, 4, 3), semente);

            assertThat(sorteados).doesNotHaveDuplicates().isSubsetOf(BANCO);
            assertThat(sorteados).hasSize(13);
        }
    }

    @Test
    void cadaSorteioPodeTrazerUmaMistura() {
        Set<Set<String>> conjuntos = new HashSet<>();
        for (long semente = 0; semente < 40; semente++) {
            conjuntos.add(Set.copyOf(sortear(new QuantidadesPorNivel(3, 2, 1), semente).stream()
                    .map(Item::nome)
                    .toList()));
        }

        assertThat(conjuntos.size()).isGreaterThan(5);
    }

    @Test
    void aOrdemTambemMudaEMisturaOsNiveis() {
        Set<List<String>> ordens = new HashSet<>();
        boolean algumaVezNaoVeioPorNivel = false;
        for (long semente = 0; semente < 40; semente++) {
            List<Item> sorteados = sortear(new QuantidadesPorNivel(3, 3, 3), semente);
            ordens.add(sorteados.stream().map(Item::nome).toList());
            List<Dificuldade> niveis = sorteados.stream().map(Item::nivel).toList();
            algumaVezNaoVeioPorNivel |= !niveis.equals(niveis.stream().sorted().toList());
        }

        assertThat(ordens.size()).isGreaterThan(10);
        assertThat(algumaVezNaoVeioPorNivel).isTrue();
    }

    @Test
    void mesmaSementeDaOMesmoSorteio() {
        assertThat(sortear(new QuantidadesPorNivel(2, 2, 2), 99))
                .isEqualTo(sortear(new QuantidadesPorNivel(2, 2, 2), 99));
    }

    @Test
    void seOBancoDeUmNivelForMenorEntramTodosOsQueExistem() {
        List<Item> sorteados = sortear(new QuantidadesPorNivel(1, 1, 10), 3);

        assertThat(sorteados.stream().filter(item -> item.nivel() == Dificuldade.DIFICIL))
                .hasSize(3);
        assertThat(sorteados).hasSize(5);
    }

    @Test
    void nivelComQuantidadeZeroNaoEntra() {
        List<Item> sorteados = sortear(new QuantidadesPorNivel(2, 0, 0), 5);

        assertThat(sorteados).hasSize(2).allMatch(item -> item.nivel() == Dificuldade.FACIL);
    }

    @Test
    void bancoVazioOuSemQuantidadeNaoSorteiaNada() {
        assertThat(SorteioDeDesafios.sortear(
                        List.<Item>of(), Item::nivel, new QuantidadesPorNivel(3, 3, 3), new Random(1)))
                .isEmpty();
        assertThat(sortear(QuantidadesPorNivel.NENHUMA, 1)).isEmpty();
    }
}

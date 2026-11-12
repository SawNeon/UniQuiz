package br.com.unimed.uniquiz.conteudo.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public final class SorteioDeDesafios {

    private SorteioDeDesafios() {}

    public static <T> List<T> sortear(
            List<T> banco,
            Function<T, Dificuldade> nivelDe,
            QuantidadesPorNivel quantidades,
            RandomGenerator aleatorio) {
        Random embaralhador = new Random(aleatorio.nextLong());
        List<T> sorteados = new ArrayList<>();
        for (Dificuldade nivel : Dificuldade.values()) {
            List<T> candidatos = doNivel(banco, nivelDe, nivel);
            sorteados.addAll(sortearAte(quantidades.de(nivel), candidatos, embaralhador));
        }
        return List.copyOf(embaralhar(sorteados, embaralhador));
    }

    private static <T> List<T> doNivel(List<T> banco, Function<T, Dificuldade> nivelDe, Dificuldade nivel) {
        return banco.stream().filter(item -> nivelDe.apply(item) == nivel).toList();
    }

    private static <T> List<T> sortearAte(int quantidade, List<T> candidatos, Random embaralhador) {
        List<T> embaralhados = embaralhar(candidatos, embaralhador);
        return embaralhados.subList(0, Math.min(quantidade, embaralhados.size()));
    }

    private static <T> List<T> embaralhar(List<T> itens, Random embaralhador) {
        List<T> embaralhados = new ArrayList<>(itens);
        Collections.shuffle(embaralhados, embaralhador);
        return embaralhados;
    }
}

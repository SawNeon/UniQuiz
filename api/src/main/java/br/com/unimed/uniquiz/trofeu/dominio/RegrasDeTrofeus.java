package br.com.unimed.uniquiz.trofeu.dominio;

import java.util.EnumSet;
import java.util.Set;

public final class RegrasDeTrofeus {

    public record Situacao(
            boolean concluiuLicao,
            boolean licaoPerfeita,
            int diasSeguidos,
            boolean concluiuModulo,
            boolean concluiuTrilha) {}

    private RegrasDeTrofeus() {}

    public static Set<CodigoDoTrofeu> merecidos(Situacao situacao) {
        Set<CodigoDoTrofeu> merecidos = EnumSet.noneOf(CodigoDoTrofeu.class);
        if (situacao.concluiuLicao()) {
            merecidos.add(CodigoDoTrofeu.PRIMEIRO_PASSO);
        }
        if (situacao.licaoPerfeita()) {
            merecidos.add(CodigoDoTrofeu.LICAO_PERFEITA);
        }
        if (situacao.diasSeguidos() >= 3) {
            merecidos.add(CodigoDoTrofeu.SEQUENCIA_3);
        }
        if (situacao.diasSeguidos() >= 7) {
            merecidos.add(CodigoDoTrofeu.SEQUENCIA_7);
        }
        if (situacao.concluiuModulo()) {
            merecidos.add(CodigoDoTrofeu.MODULO_CONCLUIDO);
        }
        if (situacao.concluiuTrilha()) {
            merecidos.add(CodigoDoTrofeu.TRILHA_COMPLETA);
        }
        return merecidos;
    }
}

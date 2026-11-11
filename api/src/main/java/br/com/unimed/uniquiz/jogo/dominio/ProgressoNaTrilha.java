package br.com.unimed.uniquiz.jogo.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProgressoNaTrilha {

    public enum SituacaoDaLicao {
        CONCLUIDA,
        DISPONIVEL,
        BLOQUEADA
    }

    private ProgressoNaTrilha() {}

    public static Map<Long, SituacaoDaLicao> situacoes(List<Long> licoesEmOrdem, Set<Long> concluidas) {
        Map<Long, SituacaoDaLicao> situacoes = new LinkedHashMap<>();
        boolean proximaLiberada = true;
        for (Long licaoId : licoesEmOrdem) {
            if (concluidas.contains(licaoId)) {
                situacoes.put(licaoId, SituacaoDaLicao.CONCLUIDA);
            } else if (proximaLiberada) {
                situacoes.put(licaoId, SituacaoDaLicao.DISPONIVEL);
                proximaLiberada = false;
            } else {
                situacoes.put(licaoId, SituacaoDaLicao.BLOQUEADA);
            }
        }
        return situacoes;
    }
}

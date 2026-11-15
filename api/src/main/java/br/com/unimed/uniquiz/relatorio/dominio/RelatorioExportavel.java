package br.com.unimed.uniquiz.relatorio.dominio;

import java.util.Arrays;
import java.util.Optional;

public enum RelatorioExportavel {
    CONFORMIDADE_PESSOAS("conformidade-pessoas", "conformidade-por-pessoa"),
    DESEMPENHO_LICOES("desempenho-licoes", "desempenho-por-licao"),
    DESEMPENHO_DESAFIOS("desempenho-desafios", "desempenho-por-desafio"),
    ENGAJAMENTO("engajamento", "engajamento-semanal");

    private final String caminho;
    private final String nomeDoArquivo;

    RelatorioExportavel(String caminho, String nomeDoArquivo) {
        this.caminho = caminho;
        this.nomeDoArquivo = nomeDoArquivo;
    }

    public String caminho() {
        return caminho;
    }

    public String nomeDoArquivo() {
        return nomeDoArquivo;
    }

    public static Optional<RelatorioExportavel> doCaminho(String caminho) {
        return Arrays.stream(values())
                .filter(relatorio -> relatorio.caminho.equals(caminho))
                .findFirst();
    }
}

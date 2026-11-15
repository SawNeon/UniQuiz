package br.com.unimed.uniquiz.relatorio.dominio;

import java.util.Collections;
import java.util.List;

public record Planilha(String nome, List<String> colunas, List<List<Object>> linhas) {

    public Planilha {
        colunas = List.copyOf(colunas);
        linhas = linhas.stream().map(Collections::unmodifiableList).toList();
    }
}

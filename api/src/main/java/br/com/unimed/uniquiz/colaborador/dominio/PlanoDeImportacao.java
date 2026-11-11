package br.com.unimed.uniquiz.colaborador.dominio;

import br.com.unimed.uniquiz.comum.Textos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PlanoDeImportacao(
        List<LinhaDaPlanilha> novos,
        List<Long> reativados,
        int mantidos,
        List<Long> inativados,
        List<String> problemas,
        List<String> homonimos) {

    public static PlanoDeImportacao montar(List<LinhaDaPlanilha> linhas, List<ColaboradorExistente> existentes) {
        List<String> problemas = new ArrayList<>();
        List<String> homonimos = new ArrayList<>();
        Map<String, LinhaDaPlanilha> validas = new LinkedHashMap<>();

        for (LinhaDaPlanilha linha : linhas) {
            if (linha.vazia()) {
                continue;
            }
            if (linha.incompleta()) {
                problemas.add("Linha %d: preencha nome, setor e empresa.".formatted(linha.numero()));
                continue;
            }
            LinhaDaPlanilha anterior = validas.putIfAbsent(chave(linha.empresa(), linha.setor(), linha.nome()), linha);
            if (anterior != null) {
                homonimos.add("Linhas %d e %d: \"%s\" aparece duas vezes em %s. Diferencie, por exemplo \"%s (%s)\"."
                        .formatted(
                                anterior.numero(),
                                linha.numero(),
                                linha.nome(),
                                linha.setor(),
                                linha.nome(),
                                linha.setor()));
            }
        }
        if (validas.isEmpty() && problemas.isEmpty()) {
            problemas.add("A planilha não tem nenhum colaborador.");
        }

        Map<String, ColaboradorExistente> existentesPorChave = new HashMap<>();
        existentes.forEach(existente ->
                existentesPorChave.put(chave(existente.empresa(), existente.setor(), existente.nome()), existente));

        List<LinhaDaPlanilha> novos = new ArrayList<>();
        List<Long> reativados = new ArrayList<>();
        Set<Long> presentes = new HashSet<>();
        int mantidos = 0;
        for (Map.Entry<String, LinhaDaPlanilha> valida : validas.entrySet()) {
            ColaboradorExistente existente = existentesPorChave.get(valida.getKey());
            if (existente == null) {
                novos.add(valida.getValue());
                continue;
            }
            presentes.add(existente.id());
            if (existente.ativo()) {
                mantidos++;
            } else {
                reativados.add(existente.id());
            }
        }

        List<Long> inativados = existentes.stream()
                .filter(ColaboradorExistente::ativo)
                .map(ColaboradorExistente::id)
                .filter(id -> !presentes.contains(id))
                .toList();

        return new PlanoDeImportacao(
                List.copyOf(novos),
                List.copyOf(reativados),
                mantidos,
                inativados,
                List.copyOf(problemas),
                List.copyOf(homonimos));
    }

    public boolean podeConfirmar() {
        return problemas.isEmpty() && homonimos.isEmpty();
    }

    public static String chave(String empresa, String setor, String nome) {
        return Textos.normalizar(empresa) + "|" + Textos.normalizar(setor) + "|" + Textos.normalizar(nome);
    }
}

package br.com.unimed.uniquiz.comum;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Textos {

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private Textos() {}

    public static String limpar(String texto) {
        if (texto == null) {
            return "";
        }
        return ESPACOS.matcher(texto.strip()).replaceAll(" ");
    }

    public static String normalizar(String texto) {
        String semAcento = ACENTOS.matcher(Normalizer.normalize(limpar(texto), Normalizer.Form.NFD))
                .replaceAll("");
        return semAcento.toUpperCase(Locale.ROOT);
    }
}

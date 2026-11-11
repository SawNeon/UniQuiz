package br.com.unimed.uniquiz.certificado.dominio;

import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

public final class CodigoDeValidacao {

    public static final int TAMANHO = 12;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern FORMATO = Pattern.compile("[" + ALFABETO + "]{" + TAMANHO + "}");

    private CodigoDeValidacao() {}

    public static String gerar(RandomGenerator aleatorio) {
        StringBuilder codigo = new StringBuilder(TAMANHO);
        for (int posicao = 0; posicao < TAMANHO; posicao++) {
            codigo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }

    public static String normalizar(String digitado) {
        return digitado == null ? "" : digitado.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean valido(String codigo) {
        return FORMATO.matcher(codigo).matches();
    }

    public static String formatado(String codigo) {
        return codigo.substring(0, 4) + "-" + codigo.substring(4, 8) + "-" + codigo.substring(8);
    }
}

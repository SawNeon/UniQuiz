package br.com.unimed.uniquiz.acesso.dominio;

import java.util.List;

public final class DescricaoDoDispositivo {

    private static final String DESCONHECIDO = "Aparelho desconhecido";

    private record Marca(String trecho, String nome) {}

    private static final List<Marca> NAVEGADORES = List.of(
            new Marca("Edg/", "Edge"),
            new Marca("OPR/", "Opera"),
            new Marca("SamsungBrowser/", "Samsung Internet"),
            new Marca("Firefox/", "Firefox"),
            new Marca("Chrome/", "Chrome"),
            new Marca("CriOS/", "Chrome"),
            new Marca("Safari/", "Safari"));

    private static final List<Marca> SISTEMAS = List.of(
            new Marca("Android", "Android"),
            new Marca("iPhone", "iPhone"),
            new Marca("iPad", "iPad"),
            new Marca("Windows", "Windows"),
            new Marca("CrOS", "ChromeOS"),
            new Marca("Mac OS X", "macOS"),
            new Marca("Linux", "Linux"));

    private DescricaoDoDispositivo() {}

    public static String de(String navegador) {
        if (navegador == null || navegador.isBlank()) {
            return DESCONHECIDO;
        }
        String nomeDoNavegador = primeiraMarca(NAVEGADORES, navegador);
        String nomeDoSistema = primeiraMarca(SISTEMAS, navegador);
        if (nomeDoNavegador == null && nomeDoSistema == null) {
            return DESCONHECIDO;
        }
        if (nomeDoSistema == null) {
            return nomeDoNavegador;
        }
        return (nomeDoNavegador == null ? "Navegador" : nomeDoNavegador) + " no " + nomeDoSistema;
    }

    private static String primeiraMarca(List<Marca> marcas, String texto) {
        return marcas.stream()
                .filter(marca -> texto.contains(marca.trecho()))
                .map(Marca::nome)
                .findFirst()
                .orElse(null);
    }
}

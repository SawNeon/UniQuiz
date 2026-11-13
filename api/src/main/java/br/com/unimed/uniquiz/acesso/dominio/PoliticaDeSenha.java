package br.com.unimed.uniquiz.acesso.dominio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PoliticaDeSenha {

    public static final int TAMANHO_MINIMO = 12;
    public static final int TAMANHO_MAXIMO = 128;
    private static final int TAMANHO_MINIMO_DO_USUARIO_PARA_COMPARAR = 3;
    private static final int TAMANHO_MAXIMO_DO_PADRAO_REPETIDO = 4;

    private static final String SEQUENCIAS_CONHECIDAS = String.join(
            " ",
            "0123456789012345678901234567890123456789",
            "9876543210987654321098765432109876543210",
            "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz",
            "zyxwvutsrqponmlkjihgfedcbazyxwvutsrqponmlkjihgfedcba",
            "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklzxcvbnm",
            "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik9ol0p");

    private static final Set<String> PALAVRAS_PREVISIVEIS = Set.of(
            "senha",
            "password",
            "admin",
            "administrador",
            "administrator",
            "unimed",
            "uniquiz",
            "qwerty",
            "qwertyuiop",
            "asdfgh",
            "abc",
            "abcdef",
            "brasil",
            "mudar",
            "trocar",
            "teste",
            "test",
            "welcome",
            "bemvindo",
            "iloveyou",
            "letmein",
            "acesso",
            "segredo",
            "usuario",
            "login",
            "master",
            "root");

    private static final Pattern SO_NUMEROS_E_SIMBOLOS = Pattern.compile("[^\\p{L}]");
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}");

    private PoliticaDeSenha() {}

    public static List<String> problemas(String senha, String usuario) {
        List<String> problemas = new ArrayList<>();
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            problemas.add("A senha precisa ter pelo menos %d caracteres.".formatted(TAMANHO_MINIMO));
            return problemas;
        }
        if (senha.length() > TAMANHO_MAXIMO) {
            problemas.add("A senha pode ter no máximo %d caracteres.".formatted(TAMANHO_MAXIMO));
        }
        if (contemOUsuario(senha, usuario)) {
            problemas.add("A senha não pode conter o nome de usuário.");
        }
        if (previsivel(senha)) {
            problemas.add("Essa senha é fácil de adivinhar. Use uma frase ou uma combinação menos comum.");
        }
        return problemas;
    }

    public static boolean aceita(String senha, String usuario) {
        return problemas(senha, usuario).isEmpty();
    }

    private static boolean contemOUsuario(String senha, String usuario) {
        if (usuario == null || usuario.strip().length() < TAMANHO_MINIMO_DO_USUARIO_PARA_COMPARAR) {
            return false;
        }
        return simplificar(senha).contains(simplificar(usuario.strip()));
    }

    private static boolean previsivel(String senha) {
        String simplificada = simplificar(senha);
        String soLetras = SO_NUMEROS_E_SIMBOLOS.matcher(simplificada).replaceAll("");
        return repeteUmPadraoCurto(simplificada)
                || SEQUENCIAS_CONHECIDAS.contains(simplificada)
                || PALAVRAS_PREVISIVEIS.contains(soLetras);
    }

    private static boolean repeteUmPadraoCurto(String texto) {
        for (int tamanho = 1; tamanho <= TAMANHO_MAXIMO_DO_PADRAO_REPETIDO; tamanho++) {
            if (texto.length() % tamanho == 0
                    && texto.equals(texto.substring(0, tamanho).repeat(texto.length() / tamanho))) {
                return true;
            }
        }
        return false;
    }

    private static String simplificar(String texto) {
        String semAcento = ACENTOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD))
                .replaceAll("");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}

package br.com.unimed.uniquiz.colaborador.dominio;

import br.com.unimed.uniquiz.comum.Textos;
import java.util.regex.Pattern;

public record LinhaDaPlanilha(int numero, String nome, String setor, String empresa, String pin) {

    public static final int TAMANHO_DO_PIN = 3;

    private static final Pattern SO_NUMEROS = Pattern.compile("\\d{1,%d}".formatted(TAMANHO_DO_PIN));

    public LinhaDaPlanilha {
        nome = Textos.limpar(nome);
        setor = Textos.limpar(setor);
        empresa = Textos.limpar(empresa);
        pin = completarPin(pin == null ? "" : pin.replaceAll("\\s", ""));
    }

    private static String completarPin(String pin) {
        if (!SO_NUMEROS.matcher(pin).matches()) {
            return pin;
        }
        return "0".repeat(TAMANHO_DO_PIN - pin.length()) + pin;
    }

    public boolean vazia() {
        return nome.isEmpty() && setor.isEmpty() && empresa.isEmpty() && pin.isEmpty();
    }

    public boolean incompleta() {
        return nome.isEmpty() || setor.isEmpty() || empresa.isEmpty();
    }

    public boolean pinValido() {
        return pin.length() == TAMANHO_DO_PIN && SO_NUMEROS.matcher(pin).matches();
    }
}

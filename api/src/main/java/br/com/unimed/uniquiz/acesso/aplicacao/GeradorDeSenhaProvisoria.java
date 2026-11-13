package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.PoliticaDeSenha;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

@Component
public class GeradorDeSenhaProvisoria {

    private static final String ALFABETO_SEM_CARACTERES_AMBIGUOS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int BLOCOS = 4;
    private static final int CARACTERES_POR_BLOCO = 4;

    private final RandomGenerator aleatorio;

    public GeradorDeSenhaProvisoria() {
        this(new SecureRandom());
    }

    GeradorDeSenhaProvisoria(RandomGenerator aleatorio) {
        this.aleatorio = aleatorio;
    }

    public String gerar(String usuario) {
        String senha;
        do {
            senha = sortear();
        } while (!PoliticaDeSenha.aceita(senha, usuario));
        return senha;
    }

    private String sortear() {
        StringBuilder senha = new StringBuilder();
        for (int bloco = 0; bloco < BLOCOS; bloco++) {
            if (bloco > 0) {
                senha.append('-');
            }
            for (int caractere = 0; caractere < CARACTERES_POR_BLOCO; caractere++) {
                senha.append(ALFABETO_SEM_CARACTERES_AMBIGUOS.charAt(
                        aleatorio.nextInt(ALFABETO_SEM_CARACTERES_AMBIGUOS.length())));
            }
        }
        return senha.toString();
    }
}

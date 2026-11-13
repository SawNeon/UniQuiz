package br.com.unimed.uniquiz.acesso.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.acesso.dominio.PoliticaDeSenha;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeradorDeSenhaProvisoriaTest {

    @Test
    void geraSenhasEmBlocosQueAPoliticaAceita() {
        GeradorDeSenhaProvisoria gerador = new GeradorDeSenhaProvisoria(new Random(42));

        for (int vez = 0; vez < 200; vez++) {
            String senha = gerador.gerar("maria");
            assertThat(senha).matches("[A-HJ-NP-Za-km-z2-9]{4}(-[A-HJ-NP-Za-km-z2-9]{4}){3}");
            assertThat(PoliticaDeSenha.aceita(senha, "maria")).isTrue();
        }
    }

    @Test
    void naoRepeteSenhas() {
        GeradorDeSenhaProvisoria gerador = new GeradorDeSenhaProvisoria();
        Set<String> geradas = new HashSet<>();

        for (int vez = 0; vez < 500; vez++) {
            geradas.add(gerador.gerar("maria"));
        }

        assertThat(geradas).hasSize(500);
    }
}

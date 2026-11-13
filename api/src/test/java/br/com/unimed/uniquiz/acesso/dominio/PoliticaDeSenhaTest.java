package br.com.unimed.uniquiz.acesso.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PoliticaDeSenhaTest {

    @Test
    void aceitaUmaFraseLongaSemExigirSimbolos() {
        assertThat(PoliticaDeSenha.aceita("cafe com leite na varanda", "maria")).isTrue();
    }

    @Test
    void recusaSenhaCurtaEExplicaOMinimo() {
        assertThat(PoliticaDeSenha.problemas("curta123", "maria"))
                .containsExactly("A senha precisa ter pelo menos 12 caracteres.");
        assertThat(PoliticaDeSenha.problemas(null, "maria")).hasSize(1);
    }

    @Test
    void recusaSenhaAcimaDoMaximo() {
        assertThat(PoliticaDeSenha.problemas("ab cd ef gh ".repeat(11), "maria"))
                .contains("A senha pode ter no máximo 128 caracteres.");
    }

    @Test
    void recusaSenhaQueContemOUsuarioMesmoComAcentoEMaiuscula() {
        assertThat(PoliticaDeSenha.problemas("minha JOÃO senha boa", "joao"))
                .contains("A senha não pode conter o nome de usuário.");
    }

    @Test
    void ignoraUsuarioCurtoDemaisParaComparar() {
        assertThat(PoliticaDeSenha.aceita("um dia de sol forte", "di")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"aaaaaaaaaaaa", "abababababab", "123412341234", "123456789012", "qwertyuiopas", "Senha@123456"})
    void recusaSenhasPrevisiveis(String senha) {
        assertThat(PoliticaDeSenha.problemas(senha, "maria"))
                .contains("Essa senha é fácil de adivinhar. Use uma frase ou uma combinação menos comum.");
    }
}

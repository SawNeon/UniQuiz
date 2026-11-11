package br.com.unimed.uniquiz.conteudo.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import br.com.unimed.uniquiz.conteudo.dominio.TipoDesafio;
import org.junit.jupiter.api.Test;

class DesafioTest {

    private static Desafio.DadosDoDesafio dados(String enunciado, String dica) {
        return new Desafio.DadosDoDesafio(
                TipoDesafio.VERDADEIRO_OU_FALSO,
                " " + enunciado + " ",
                Dificuldade.FACIL,
                10,
                " Explica. ",
                dica,
                "{}");
    }

    @Test
    void rascunhoEditadoContinuaNaPrimeiraVersao() {
        Desafio desafio = new Desafio(1L, dados("Original", null), 1);

        desafio.atualizar(dados("Editado", null));

        assertThat(desafio.getVersao()).isEqualTo(1);
        assertThat(desafio.getEnunciado()).isEqualTo("Editado");
        assertThat(desafio.getExplicacao()).isEqualTo("Explica.");
    }

    @Test
    void publicadoEditadoGanhaNovaVersao() {
        Desafio desafio = new Desafio(1L, dados("Original", null), 1);
        desafio.mudarStatus(StatusPublicacao.PUBLICADO);

        desafio.atualizar(dados("Editado", null));
        desafio.atualizar(dados("Editado de novo", null));

        assertThat(desafio.getVersao()).isEqualTo(3);
    }

    @Test
    void copiaComecaComoRascunhoNaPrimeiraVersao() {
        Desafio desafio = new Desafio(1L, dados("Original", " Uma dica "), 1);
        desafio.mudarStatus(StatusPublicacao.PUBLICADO);
        desafio.atualizar(dados("Editado", " Uma dica "));

        Desafio copia = desafio.copiar(7);

        assertThat(copia.getStatus()).isEqualTo(StatusPublicacao.RASCUNHO);
        assertThat(copia.getVersao()).isEqualTo(1);
        assertThat(copia.getOrdem()).isEqualTo(7);
        assertThat(copia.getDica()).isEqualTo("Uma dica");
    }

    @Test
    void dicaEmBrancoViraNula() {
        assertThat(new Desafio(1L, dados("X", "   "), 1).getDica()).isNull();
    }
}

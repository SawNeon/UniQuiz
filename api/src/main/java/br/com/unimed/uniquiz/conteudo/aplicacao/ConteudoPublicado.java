package br.com.unimed.uniquiz.conteudo.aplicacao;

import br.com.unimed.uniquiz.conteudo.dominio.ConteudoDesafio;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoPublico;
import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.TipoDesafio;
import java.util.List;
import java.util.random.RandomGenerator;

public final class ConteudoPublicado {

    private ConteudoPublicado() {}

    public record Modulo(Long id, String titulo, String descricao, boolean temMaterial, List<Licao> licoes) {}

    public record Licao(Long id, Long moduloId, String titulo) {}

    public record Material(Long moduloId, String titulo, String conteudo) {}

    public record DesafioEmJogo(
            Long id,
            Long licaoId,
            int versao,
            TipoDesafio tipo,
            String enunciado,
            String dica,
            Dificuldade dificuldade,
            int pontos,
            String explicacao,
            ConteudoDesafio conteudo) {

        public DesafioPublico publico(RandomGenerator aleatorio) {
            return new DesafioPublico(id, tipo, enunciado, dica, dificuldade, conteudo.paraColaborador(aleatorio));
        }
    }

    public record DesafioPublico(
            Long id,
            TipoDesafio tipo,
            String enunciado,
            String dica,
            Dificuldade dificuldade,
            ConteudoPublico conteudo) {}
}

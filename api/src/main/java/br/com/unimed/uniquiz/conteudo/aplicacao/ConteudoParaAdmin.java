package br.com.unimed.uniquiz.conteudo.aplicacao;

import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import br.com.unimed.uniquiz.conteudo.dominio.TipoDesafio;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ConteudoParaAdmin {

    private ConteudoParaAdmin() {}

    public record DadosDoModulo(
            @NotBlank @Size(max = 150) String titulo,
            @Size(max = 500) String descricao,
            @Size(max = 20_000) String materialEstudo) {}

    public record DadosDaLicao(@NotBlank @Size(max = 150) String titulo) {}

    public record DadosDoDesafio(
            @NotNull TipoDesafio tipo,
            @NotBlank @Size(max = 1000) String enunciado,
            @NotNull Dificuldade dificuldade,
            @Min(1) @Max(100) int pontos,
            @NotBlank @Size(max = 2000) String explicacao,
            @Size(max = 500) String dica,
            @NotNull JsonNode conteudo) {}

    public record NovoStatus(@NotNull StatusPublicacao status) {}

    public record NovaOrdem(@NotNull List<Long> ids) {}

    public record Modulo(
            Long id,
            String titulo,
            String descricao,
            String materialEstudo,
            int ordem,
            StatusPublicacao status,
            List<Licao> licoes) {}

    public record Licao(
            Long id, Long moduloId, String titulo, int ordem, StatusPublicacao status, long desafiosPublicados) {}

    public record Desafio(
            Long id,
            Long licaoId,
            TipoDesafio tipo,
            String enunciado,
            Dificuldade dificuldade,
            int pontos,
            String explicacao,
            String dica,
            JsonNode conteudo,
            int versao,
            int ordem,
            StatusPublicacao status) {}
}

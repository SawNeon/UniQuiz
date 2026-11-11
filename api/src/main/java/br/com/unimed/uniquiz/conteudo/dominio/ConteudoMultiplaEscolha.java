package br.com.unimed.uniquiz.conteudo.dominio;

import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.exigir;
import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.preenchido;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.util.List;
import java.util.random.RandomGenerator;

public record ConteudoMultiplaEscolha(List<Alternativa> alternativas) implements ConteudoDesafio {

    public record Alternativa(String id, String texto, boolean correta, String motivo) {}

    public ConteudoMultiplaEscolha {
        alternativas = alternativas == null ? List.of() : List.copyOf(alternativas);
    }

    @Override
    public void validar() {
        exigir(alternativas.size() >= 2 && alternativas.size() <= 6, "Cadastre de 2 a 6 alternativas.");
        exigir(
                alternativas.stream().filter(Alternativa::correta).count() == 1,
                "Marque exatamente uma alternativa correta.");
        exigir(
                alternativas.stream().map(Alternativa::id).distinct().count() == alternativas.size(),
                "Cada alternativa precisa de um identificador diferente.");
        for (Alternativa alternativa : alternativas) {
            exigir(
                    preenchido(alternativa.id()) && preenchido(alternativa.texto()),
                    "Preencha o texto de cada alternativa.");
            exigir(
                    alternativa.correta() || preenchido(alternativa.motivo()),
                    "Explique por que a alternativa \"%s\" está errada.".formatted(alternativa.texto()));
        }
    }

    @Override
    public ConteudoPublico paraColaborador(RandomGenerator aleatorio) {
        return new ConteudoPublico.MultiplaEscolha(alternativas.stream()
                .map(alternativa -> new ConteudoPublico.AlternativaPublica(alternativa.id(), alternativa.texto()))
                .toList());
    }

    @Override
    public Correcao corrigir(RespostaDoColaborador resposta) {
        var escolha = Validacoes.respostaDoTipo(resposta, RespostaDoColaborador.MultiplaEscolha.class);
        Alternativa escolhida = alternativas.stream()
                .filter(alternativa -> alternativa.id().equals(escolha.alternativaId()))
                .findFirst()
                .orElseThrow(() -> new ErroDeNegocio("Escolha uma das alternativas do desafio."));
        String respostaCerta = alternativas.stream()
                .filter(Alternativa::correta)
                .map(Alternativa::texto)
                .findFirst()
                .orElseThrow();
        return escolhida.correta() ? Correcao.acerto(respostaCerta) : Correcao.erro(escolhida.motivo(), respostaCerta);
    }
}

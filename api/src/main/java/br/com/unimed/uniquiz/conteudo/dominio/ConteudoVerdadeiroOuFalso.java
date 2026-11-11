package br.com.unimed.uniquiz.conteudo.dominio;

import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.exigir;
import static br.com.unimed.uniquiz.conteudo.dominio.Validacoes.preenchido;

import java.util.random.RandomGenerator;

public record ConteudoVerdadeiroOuFalso(String afirmacao, boolean verdadeira, String motivoDoErro)
        implements ConteudoDesafio {

    @Override
    public void validar() {
        exigir(preenchido(afirmacao), "Escreva a afirmação.");
        exigir(preenchido(motivoDoErro), "Explique o que a pessoa precisa entender se errar.");
    }

    @Override
    public ConteudoPublico paraColaborador(RandomGenerator aleatorio) {
        return new ConteudoPublico.VerdadeiroOuFalso(afirmacao);
    }

    @Override
    public Correcao corrigir(RespostaDoColaborador resposta) {
        var escolha = Validacoes.respostaDoTipo(resposta, RespostaDoColaborador.VerdadeiroOuFalso.class);
        exigir(escolha.valor() != null, "Escolha verdadeiro ou falso.");
        String respostaCerta = verdadeira ? "Verdadeiro" : "Falso";
        return escolha.valor() == verdadeira
                ? Correcao.acerto(respostaCerta)
                : Correcao.erro(motivoDoErro, respostaCerta);
    }
}

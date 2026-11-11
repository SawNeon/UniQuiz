package br.com.unimed.uniquiz.conteudo.dominio;

import java.util.random.RandomGenerator;

public sealed interface ConteudoDesafio
        permits ConteudoMultiplaEscolha, ConteudoVerdadeiroOuFalso, ConteudoCompletarClicando {

    void validar();

    ConteudoPublico paraColaborador(RandomGenerator aleatorio);

    Correcao corrigir(RespostaDoColaborador resposta);
}

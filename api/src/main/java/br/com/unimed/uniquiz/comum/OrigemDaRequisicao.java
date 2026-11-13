package br.com.unimed.uniquiz.comum;

import java.util.Optional;

public interface OrigemDaRequisicao {

    Optional<String> ipAtual();

    Optional<String> navegadorAtual();
}

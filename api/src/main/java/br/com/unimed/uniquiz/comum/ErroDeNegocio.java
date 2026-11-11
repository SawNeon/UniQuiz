package br.com.unimed.uniquiz.comum;

public class ErroDeNegocio extends RuntimeException {

    public ErroDeNegocio(String mensagem) {
        super(mensagem);
    }
}

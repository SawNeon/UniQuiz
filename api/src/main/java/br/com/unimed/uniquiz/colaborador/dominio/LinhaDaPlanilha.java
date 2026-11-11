package br.com.unimed.uniquiz.colaborador.dominio;

import br.com.unimed.uniquiz.comum.Textos;

public record LinhaDaPlanilha(int numero, String nome, String setor, String empresa) {

    public LinhaDaPlanilha {
        nome = Textos.limpar(nome);
        setor = Textos.limpar(setor);
        empresa = Textos.limpar(empresa);
    }

    public boolean vazia() {
        return nome.isEmpty() && setor.isEmpty() && empresa.isEmpty();
    }

    public boolean incompleta() {
        return nome.isEmpty() || setor.isEmpty() || empresa.isEmpty();
    }
}

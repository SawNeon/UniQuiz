package br.com.unimed.uniquiz.colaborador.aplicacao;

import br.com.unimed.uniquiz.colaborador.dominio.PlanoDeImportacao;
import java.util.List;

public record ResumoDaImportacao(
        int novos,
        int mantidos,
        int reativados,
        int inativados,
        List<String> problemas,
        List<String> homonimos,
        boolean podeConfirmar) {

    static ResumoDaImportacao de(PlanoDeImportacao plano) {
        return new ResumoDaImportacao(
                plano.novos().size(),
                plano.mantidos(),
                plano.reativados().size(),
                plano.inativados().size(),
                plano.problemas(),
                plano.homonimos(),
                plano.podeConfirmar());
    }
}

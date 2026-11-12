package br.com.unimed.uniquiz.colaborador.web;

import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores.ColaboradorDaEntrada;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores.ItemDaLista;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class EntradaController {

    private final ServicoDeColaboradores colaboradores;

    EntradaController(ServicoDeColaboradores colaboradores) {
        this.colaboradores = colaboradores;
    }

    @GetMapping("/empresas")
    List<ItemDaLista> empresas() {
        return colaboradores.empresas();
    }

    @GetMapping("/empresas/{empresaId}/setores")
    List<ItemDaLista> setores(@PathVariable Long empresaId) {
        return colaboradores.setoresDaEmpresa(empresaId);
    }

    @GetMapping("/setores/{setorId}/colaboradores")
    List<ColaboradorDaEntrada> colaboradoresDoSetor(@PathVariable Long setorId) {
        return colaboradores.ativosDoSetor(setorId);
    }
}

package br.com.unimed.uniquiz.conteudo.web;

import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoPublicado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ServicoDeConteudoPublicado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EstudoController {

    private final ServicoDeConteudoPublicado conteudo;

    EstudoController(ServicoDeConteudoPublicado conteudo) {
        this.conteudo = conteudo;
    }

    @GetMapping("/api/modulos/{moduloId}/estudo")
    ConteudoPublicado.Material material(@PathVariable Long moduloId) {
        return conteudo.material(moduloId);
    }
}

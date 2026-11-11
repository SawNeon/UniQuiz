package br.com.unimed.uniquiz.classificacao.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeClassificacao;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeClassificacao.Classificacao;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classificacao")
class ClassificacaoController {

    private final ServicoDeClassificacao classificacao;

    ClassificacaoController(ServicoDeClassificacao classificacao) {
        this.classificacao = classificacao;
    }

    @GetMapping
    Classificacao porPontos(
            @AuthenticationPrincipal UsuarioLogado usuario, @RequestParam(required = false) Long temporadaId) {
        return classificacao.porPontos(usuario.id(), temporadaId);
    }

    @GetMapping("/rapidos")
    Classificacao maisRapidos(@AuthenticationPrincipal UsuarioLogado usuario, @RequestParam Long licaoId) {
        return classificacao.maisRapidos(usuario.id(), licaoId);
    }
}

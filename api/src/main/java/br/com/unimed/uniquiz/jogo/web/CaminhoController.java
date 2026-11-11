package br.com.unimed.uniquiz.jogo.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeProgresso;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeProgresso.ModuloNoCaminho;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CaminhoController {

    private final ServicoDeProgresso progresso;

    CaminhoController(ServicoDeProgresso progresso) {
        this.progresso = progresso;
    }

    @GetMapping("/api/caminho")
    List<ModuloNoCaminho> caminho(@AuthenticationPrincipal UsuarioLogado usuario) {
        return progresso.caminho(usuario.id());
    }
}

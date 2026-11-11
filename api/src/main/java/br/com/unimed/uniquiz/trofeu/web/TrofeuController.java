package br.com.unimed.uniquiz.trofeu.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.trofeu.aplicacao.ServicoDeTrofeus;
import br.com.unimed.uniquiz.trofeu.aplicacao.ServicoDeTrofeus.TrofeuDoColaborador;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TrofeuController {

    private final ServicoDeTrofeus trofeus;

    TrofeuController(ServicoDeTrofeus trofeus) {
        this.trofeus = trofeus;
    }

    @GetMapping("/api/trofeus")
    List<TrofeuDoColaborador> meus(@AuthenticationPrincipal UsuarioLogado usuario) {
        return trofeus.doColaborador(usuario.id());
    }
}

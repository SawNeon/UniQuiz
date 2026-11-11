package br.com.unimed.uniquiz.perfil.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.perfil.aplicacao.ServicoDePerfil;
import br.com.unimed.uniquiz.perfil.aplicacao.ServicoDePerfil.Perfil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PerfilController {

    private final ServicoDePerfil perfis;

    PerfilController(ServicoDePerfil perfis) {
        this.perfis = perfis;
    }

    @GetMapping("/api/eu")
    Perfil eu(@AuthenticationPrincipal UsuarioLogado usuario) {
        return perfis.doColaborador(usuario.id());
    }
}

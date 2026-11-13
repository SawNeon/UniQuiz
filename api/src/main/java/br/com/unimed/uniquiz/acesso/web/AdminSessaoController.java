package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeLoginAdmin;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeSessao;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel.ContaLogada;
import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
class AdminSessaoController {

    record PedidoDeLogin(@NotBlank @Size(max = 60) String usuario, @NotBlank @Size(max = 200) String senha) {}

    private final ServicoDeLoginAdmin login;
    private final ServicoDeSessao sessoes;
    private final ServicoDeUsuariosDoPainel usuarios;
    private final CookiesDeSessao cookies;

    AdminSessaoController(
            ServicoDeLoginAdmin login,
            ServicoDeSessao sessoes,
            ServicoDeUsuariosDoPainel usuarios,
            CookiesDeSessao cookies) {
        this.login = login;
        this.sessoes = sessoes;
        this.usuarios = usuarios;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    ResponseEntity<Void> entrar(@Valid @RequestBody PedidoDeLogin pedido) {
        String token = login.entrar(pedido.usuario(), pedido.senha());
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookies.administrador(token, sessoes.duracaoSessaoAdmin())
                                .toString())
                .build();
    }

    @GetMapping("/eu")
    ContaLogada eu(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuarios.conta(usuario.id());
    }

    @DeleteMapping("/sessao")
    ResponseEntity<Void> sair(HttpServletRequest requisicao) {
        CookiesDeSessao.ler(requisicao, CookiesDeSessao.ADMINISTRADOR).ifPresent(sessoes::encerrar);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.apagarAdministrador().toString())
                .build();
    }
}

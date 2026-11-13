package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeMinhaConta;
import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.acesso.dominio.PoliticaDeSenha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/minha-conta")
class MinhaContaController {

    record PedidoDeTrocaDeSenha(
            @NotBlank @Size(max = PoliticaDeSenha.TAMANHO_MAXIMO) String senhaAtual,
            @NotBlank @Size(max = PoliticaDeSenha.TAMANHO_MAXIMO) String novaSenha) {}

    private final ServicoDeMinhaConta conta;

    MinhaContaController(ServicoDeMinhaConta conta) {
        this.conta = conta;
    }

    @PostMapping("/senha")
    ResponseEntity<Void> trocarSenha(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @Valid @RequestBody PedidoDeTrocaDeSenha pedido,
            HttpServletRequest requisicao) {
        String tokenAtual =
                CookiesDeSessao.ler(requisicao, CookiesDeSessao.ADMINISTRADOR).orElseThrow();
        conta.trocarSenha(usuario.id(), pedido.senhaAtual(), pedido.novaSenha(), tokenAtual);
        return ResponseEntity.noContent().build();
    }
}

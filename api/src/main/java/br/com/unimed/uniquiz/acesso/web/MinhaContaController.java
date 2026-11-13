package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeMinhaConta;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeSessao;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeSessao.SessaoAtiva;
import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.acesso.dominio.PoliticaDeSenha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ServicoDeSessao sessoes;

    MinhaContaController(ServicoDeMinhaConta conta, ServicoDeSessao sessoes) {
        this.conta = conta;
        this.sessoes = sessoes;
    }

    @PostMapping("/senha")
    ResponseEntity<Void> trocarSenha(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @Valid @RequestBody PedidoDeTrocaDeSenha pedido,
            HttpServletRequest requisicao) {
        conta.trocarSenha(usuario.id(), pedido.senhaAtual(), pedido.novaSenha(), tokenAtual(requisicao));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessoes")
    List<SessaoAtiva> sessoes(@AuthenticationPrincipal UsuarioLogado usuario, HttpServletRequest requisicao) {
        return sessoes.sessoesDoAdministrador(usuario.id(), tokenAtual(requisicao));
    }

    @DeleteMapping("/sessoes/{sessaoId}")
    ResponseEntity<Void> encerrarSessao(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable String sessaoId) {
        sessoes.encerrarSessaoDoAdministrador(usuario.id(), sessaoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessoes/encerrar-outras")
    ResponseEntity<Void> encerrarOutras(@AuthenticationPrincipal UsuarioLogado usuario, HttpServletRequest requisicao) {
        sessoes.encerrarOutrasDoAdministrador(usuario.id(), tokenAtual(requisicao));
        return ResponseEntity.noContent().build();
    }

    private static String tokenAtual(HttpServletRequest requisicao) {
        return CookiesDeSessao.ler(requisicao, CookiesDeSessao.ADMINISTRADOR).orElseThrow();
    }
}

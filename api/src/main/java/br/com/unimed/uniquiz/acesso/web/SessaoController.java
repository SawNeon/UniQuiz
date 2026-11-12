package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeSessao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessao")
class SessaoController {

    record PedidoDeEntrada(
            @NotNull Long colaboradorId,
            @NotNull @Pattern(regexp = "\\d{3}", message = "O PIN tem 3 números.") String pin) {}

    private final ServicoDeSessao sessoes;
    private final CookiesDeSessao cookies;

    SessaoController(ServicoDeSessao sessoes, CookiesDeSessao cookies) {
        this.sessoes = sessoes;
        this.cookies = cookies;
    }

    @PostMapping
    ResponseEntity<Void> entrar(@Valid @RequestBody PedidoDeEntrada pedido) {
        String token = sessoes.entrarComoColaborador(pedido.colaboradorId(), pedido.pin());
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookies.colaborador(token, sessoes.duracaoSessaoColaborador())
                                .toString())
                .build();
    }

    @DeleteMapping
    ResponseEntity<Void> sair(HttpServletRequest requisicao) {
        CookiesDeSessao.ler(requisicao, CookiesDeSessao.COLABORADOR).ifPresent(sessoes::encerrar);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.apagarColaborador().toString())
                .build();
    }
}

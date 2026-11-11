package br.com.unimed.uniquiz.jogo.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeLicao;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeLicao.RespostaCorrigida;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeLicao.ResultadoDaLicao;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeLicao.TentativaIniciada;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LicaoController {

    record EnvioDeResposta(@NotNull Long desafioId, @NotNull JsonNode resposta) {}

    private final ServicoDeLicao licoes;

    LicaoController(ServicoDeLicao licoes) {
        this.licoes = licoes;
    }

    @PostMapping("/api/licoes/{licaoId}/tentativas")
    @ResponseStatus(HttpStatus.CREATED)
    TentativaIniciada iniciar(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long licaoId) {
        return licoes.iniciar(usuario.id(), licaoId);
    }

    @PostMapping("/api/tentativas/{tentativaId}/respostas")
    RespostaCorrigida responder(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @PathVariable Long tentativaId,
            @Valid @RequestBody EnvioDeResposta envio) {
        return licoes.responder(usuario.id(), tentativaId, envio.desafioId(), envio.resposta());
    }

    @PostMapping("/api/tentativas/{tentativaId}/concluir")
    ResultadoDaLicao concluir(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long tentativaId) {
        return licoes.concluir(usuario.id(), tentativaId);
    }
}

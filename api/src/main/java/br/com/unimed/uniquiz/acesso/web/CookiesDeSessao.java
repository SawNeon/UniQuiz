package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookiesDeSessao {

    static final String COLABORADOR = "uniquiz_sessao";
    static final String ADMINISTRADOR = "uniquiz_admin";
    private static final String CAMINHO_COLABORADOR = "/api";
    private static final String CAMINHO_ADMINISTRADOR = "/api/admin";
    private static final String MESMO_SITE_COM_LINKS = "Lax";
    private static final String SO_MESMO_SITE = "Strict";

    private final boolean seguro;

    public CookiesDeSessao(PropriedadesDoUniQuiz propriedades) {
        this.seguro = propriedades.sessao().cookieSeguro();
    }

    ResponseCookie colaborador(String token, Duration duracao) {
        return criar(COLABORADOR, token, CAMINHO_COLABORADOR, MESMO_SITE_COM_LINKS, duracao);
    }

    ResponseCookie administrador(String token, Duration duracao) {
        return criar(ADMINISTRADOR, token, CAMINHO_ADMINISTRADOR, SO_MESMO_SITE, duracao);
    }

    ResponseCookie apagarColaborador() {
        return criar(COLABORADOR, "", CAMINHO_COLABORADOR, MESMO_SITE_COM_LINKS, Duration.ZERO);
    }

    ResponseCookie apagarAdministrador() {
        return criar(ADMINISTRADOR, "", CAMINHO_ADMINISTRADOR, SO_MESMO_SITE, Duration.ZERO);
    }

    static Optional<String> ler(HttpServletRequest requisicao, String nome) {
        Cookie[] cookies = requisicao.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> nome.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(valor -> !valor.isBlank())
                .findFirst();
    }

    private ResponseCookie criar(String nome, String valor, String caminho, String mesmoSite, Duration duracao) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(seguro)
                .sameSite(mesmoSite)
                .path(caminho)
                .maxAge(duracao)
                .build();
    }
}

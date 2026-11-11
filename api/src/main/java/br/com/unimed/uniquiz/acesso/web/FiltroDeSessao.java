package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeSessao;
import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class FiltroDeSessao extends OncePerRequestFilter {

    private final ServicoDeSessao sessoes;

    FiltroDeSessao(ServicoDeSessao sessoes) {
        this.sessoes = sessoes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        boolean areaAdmin = requisicao.getRequestURI().startsWith("/api/admin/");
        String cookie = areaAdmin ? CookiesDeSessao.ADMINISTRADOR : CookiesDeSessao.COLABORADOR;

        CookiesDeSessao.ler(requisicao, cookie).flatMap(sessoes::buscar).ifPresent(FiltroDeSessao::autenticar);

        cadeia.doFilter(requisicao, resposta);
    }

    private static void autenticar(UsuarioLogado usuario) {
        var autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, List.of(new SimpleGrantedAuthority(usuario.papel())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
    }
}

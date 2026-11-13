package br.com.unimed.uniquiz.auditoria.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.CatalogoDeRotas;
import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

class InterceptadorDeAuditoria implements HandlerInterceptor {

    private static final Set<String> METODOS_QUE_ALTERAM = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ServicoDeAuditoria auditoria;

    InterceptadorDeAuditoria(ServicoDeAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest requisicao, HttpServletResponse resposta, Object manipulador, Exception erro) {
        String metodo = requisicao.getMethod();
        Object rotaModelo = requisicao.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (!METODOS_QUE_ALTERAM.contains(metodo)
                || rotaModelo == null
                || !CatalogoDeRotas.registradaPeloInterceptador(metodo, rotaModelo.toString())) {
            return;
        }
        Optional<UsuarioLogado> usuario = usuarioLogado();
        OcorrenciaDeAuditoria ocorrencia = OcorrenciaDeAuditoria.de(
                        usuario.map(ignorado -> TipoDeAtor.ADMINISTRADOR).orElse(TipoDeAtor.ANONIMO),
                        usuario.map(UsuarioLogado::id).orElse(null),
                        CatalogoDeRotas.acaoDa(metodo, rotaModelo.toString()))
                .comAlvo(CatalogoDeRotas.alvoDe(variaveisDaRota(requisicao)));
        auditoria.registrar(falhou(resposta, erro) ? ocorrencia.comFalha(motivo(resposta, erro)) : ocorrencia);
    }

    private static boolean falhou(HttpServletResponse resposta, Exception erro) {
        return erro != null || resposta.getStatus() >= HttpStatus.BAD_REQUEST.value();
    }

    private static String motivo(HttpServletResponse resposta, Exception erro) {
        return erro != null ? erro.getClass().getSimpleName() : "HTTP " + resposta.getStatus();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> variaveisDaRota(HttpServletRequest requisicao) {
        return (Map<String, String>) requisicao.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }

    private static Optional<UsuarioLogado> usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioLogado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }
}

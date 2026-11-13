package br.com.unimed.uniquiz.comum.web;

import br.com.unimed.uniquiz.comum.OrigemDaRequisicao;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
class OrigemDaRequisicaoHttp implements OrigemDaRequisicao {

    @Override
    public Optional<String> ipAtual() {
        return requisicaoAtual().map(HttpServletRequest::getRemoteAddr);
    }

    @Override
    public Optional<String> navegadorAtual() {
        return requisicaoAtual().map(requisicao -> requisicao.getHeader(HttpHeaders.USER_AGENT));
    }

    private static Optional<HttpServletRequest> requisicaoAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return Optional.of(atributos.getRequest());
        }
        return Optional.empty();
    }
}

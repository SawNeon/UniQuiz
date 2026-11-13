package br.com.unimed.uniquiz.auditoria.web;

import br.com.unimed.uniquiz.auditoria.aplicacao.OrigemDaRequisicao;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
class OrigemDaRequisicaoHttp implements OrigemDaRequisicao {

    @Override
    public Optional<String> ipAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return Optional.ofNullable(atributos.getRequest().getRemoteAddr());
        }
        return Optional.empty();
    }
}

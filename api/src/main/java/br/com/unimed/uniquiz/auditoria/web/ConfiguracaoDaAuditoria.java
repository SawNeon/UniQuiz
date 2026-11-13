package br.com.unimed.uniquiz.auditoria.web;

import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeAuditoria;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ConfiguracaoDaAuditoria implements WebMvcConfigurer {

    private final ServicoDeAuditoria auditoria;

    ConfiguracaoDaAuditoria(ServicoDeAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new InterceptadorDeAuditoria(auditoria)).addPathPatterns("/api/admin/**");
    }
}

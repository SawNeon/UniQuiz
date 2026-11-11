package br.com.unimed.uniquiz.comum.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltroDeLimiteDeRequisicoesTest {

    private final FiltroDeLimiteDeRequisicoes filtro =
            new FiltroDeLimiteDeRequisicoes(new PropriedadesDoUniQuiz.LimiteRequisicoes(3, 1), new ObjectMapper());

    @Test
    void passaDoLimiteRecebe429ComMensagem() throws Exception {
        for (int pedido = 0; pedido < 3; pedido++) {
            assertThat(executar("GET", "/api/caminho", "10.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse bloqueada = executar("GET", "/api/caminho", "10.0.0.1");

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getContentType()).startsWith("application/problem+json");
        assertThat(bloqueada.getContentAsString()).contains("Aguarde um minuto");
    }

    @Test
    void cadaIpTemOProprioLimite() throws Exception {
        for (int pedido = 0; pedido < 3; pedido++) {
            executar("GET", "/api/caminho", "10.0.0.1");
        }

        assertThat(executar("GET", "/api/caminho", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void loginDoAdminTemLimiteProprioEMaisRigido() throws Exception {
        assertThat(executar("POST", "/api/admin/login", "10.0.0.3").getStatus()).isEqualTo(200);
        assertThat(executar("POST", "/api/admin/login", "10.0.0.3").getStatus()).isEqualTo(429);
        assertThat(executar("GET", "/api/caminho", "10.0.0.3").getStatus()).isEqualTo(200);
    }

    @Test
    void naoLimitaForaDaApiEEsvaziarLiberaDeNovo() throws Exception {
        for (int pedido = 0; pedido < 5; pedido++) {
            assertThat(executar("GET", "/actuator/health", "10.0.0.4").getStatus())
                    .isEqualTo(200);
        }
        executar("POST", "/api/admin/login", "10.0.0.5");
        filtro.esvaziar();

        assertThat(executar("POST", "/api/admin/login", "10.0.0.5").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse executar(String metodo, String caminho, String ip) throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest(metodo, caminho);
        requisicao.setRemoteAddr(ip);
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, new MockFilterChain());
        return resposta;
    }
}

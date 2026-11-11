package br.com.unimed.uniquiz;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class SegurancaTest extends TesteDeIntegracao {

    @ParameterizedTest
    @ValueSource(strings = {"/api/caminho", "/api/eu", "/api/classificacao", "/api/trofeus", "/api/certificados"})
    void rotasDoColaboradorExigemSessao(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/modulos", "/api/admin/colaboradores", "/api/admin/temporadas", "/api/admin/eu"})
    void rotasDoAdminExigemSessaoDeAdmin(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        mvc.perform(get(rota).cookie(entrarComo("Ana Exemplo"))).andExpect(status().isUnauthorized());
    }

    @Test
    void sessaoDeAdminNaoServeNaAreaDoColaborador() throws Exception {
        Cookie admin = entrarComoAdmin();

        mvc.perform(get("/api/caminho").cookie(new Cookie("uniquiz_sessao", admin.getValue())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listasDaTelaDeEntradaSaoPublicas() throws Exception {
        mvc.perform(get("/api/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Unimed Exemplo"));
    }

    @Test
    void naoSouEuEncerraASessao() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        mvc.perform(get("/api/eu").cookie(sessao)).andExpect(status().isOk());

        mvc.perform(delete("/api/sessao").cookie(sessao))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        mvc.perform(get("/api/eu").cookie(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void cookieDeSessaoEHttpOnlyESameSite() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", idDoColaborador("Ana Exemplo"))))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    void colaboradorInativadoPerdeASessaoENaoEntraDeNovo() throws Exception {
        Cookie sessao = entrarComo("Carla Demonstração");
        long carla = idDoColaborador("Carla Demonstração");

        mvc.perform(comJson(
                        patch("/api/admin/colaboradores/{id}", carla).cookie(entrarComoAdmin()),
                        Map.of("nome", "Carla Demonstração", "ativo", false)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/eu").cookie(sessao)).andExpect(status().isUnauthorized());
        mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", carla)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminEBloqueadoDepoisDeCincoSenhasErradas() throws Exception {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "admin", "senha", "errada")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos."));
        }

        mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "admin", "senha", SENHA_DO_ADMIN)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("bloqueado")));
    }

    @Test
    void usuarioInexistenteRecebeAMesmaMensagemDeSenhaErrada() throws Exception {
        mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "ninguem", "senha", "qualquer")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos."));
    }

    @Test
    void errosDeValidacaoVoltamNoFormatoProblemDetailComOsCampos() throws Exception {
        mvc.perform(post("/api/sessao").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("problem+json")))
                .andExpect(jsonPath("$.campos.colaboradorId").exists());
    }

    @Test
    void respostasTrazemCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("/api/empresas"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void saudeDaAplicacaoEPublica() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}

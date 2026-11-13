package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PinTest extends TesteDeIntegracao {

    private Map<String, Object> entrada(String nome, String pin) {
        return Map.of("colaboradorId", idDoColaborador(nome), "pin", pin);
    }

    @Test
    void primeiroAcessoCriaOPinEGuardaSoOHash() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482"))).andExpect(status().isNoContent());

        String hash =
                jdbc.queryForObject("SELECT pin_hash FROM colaborador WHERE nome = ?", String.class, "Ana Exemplo");
        assertThat(hash).isNotBlank().doesNotContain("482");
    }

    @Test
    void depoisDoPrimeiroAcessoPrecisaDoMesmoPin() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482"))).andExpect(status().isNoContent());

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "111")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("PIN incorreto."));
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482"))).andExpect(status().isNoContent());
    }

    @Test
    void listaDaEntradaDizSeAPessoaJaTemPin() throws Exception {
        long setor = jdbc.queryForObject("SELECT setor_id FROM colaborador WHERE nome = ?", Long.class, "Ana Exemplo");
        mvc.perform(get("/api/setores/{id}/colaboradores", setor).param("busca", "ana"))
                .andExpect(jsonPath("$[?(@.nome == 'Ana Exemplo')].temPin").value(false));

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482"))).andExpect(status().isNoContent());

        mvc.perform(get("/api/setores/{id}/colaboradores", setor).param("busca", "ana"))
                .andExpect(jsonPath("$[?(@.nome == 'Ana Exemplo')].temPin").value(true));
    }

    @Test
    void bloqueiaDepoisDeCincoPinsErradosEOAdminDestrava() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482"))).andExpect(status().isNoContent());
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "000")))
                    .andExpect(status().isUnprocessableEntity());
        }

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("bloqueado")));

        mvc.perform(post("/api/admin/colaboradores/{id}/redefinir-pin", idDoColaborador("Ana Exemplo"))
                        .cookie(entrarComoAdmin()))
                .andExpect(status().isNoContent());

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "777"))).andExpect(status().isNoContent());
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482")))
                .andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "1234", "abc", "", "1 2"})
    void pinPrecisaTerTresNumeros(String pin) throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", pin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pin").exists());
    }

    @Test
    void chutesEmParaleloNaoPassamDoLimiteDeCincoErros() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482"))).andExpect(status().isNoContent());

        ExecutorService pool = Executors.newFixedThreadPool(30);
        try {
            List<Future<String>> respostas = IntStream.range(0, 60)
                    .mapToObj(chute ->
                            pool.submit(() -> mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "000")))
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString()))
                    .toList();
            long chutesAvaliados = 0;
            for (Future<String> resposta : respostas) {
                if (resposta.get().contains("PIN incorreto")) {
                    chutesAvaliados++;
                }
            }
            assertThat(chutesAvaliados).isEqualTo(5);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void semPinNaoEntra() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", idDoColaborador("Ana Exemplo"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCadastraColaboradorNovoCriandoOSetorQueFalta() throws Exception {
        Cookie admin = entrarComoAdmin();

        pedir(
                comJson(
                        post("/api/admin/colaboradores").cookie(admin),
                        Map.of("nome", "Diego Novo", "empresa", "Unimed Exemplo", "setor", "Financeiro")),
                status().isCreated());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa WHERE nome = ?", Integer.class, "Unimed Exemplo"))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM setor WHERE nome = ?", Integer.class, "Financeiro"))
                .isEqualTo(1);
        mvc.perform(comJson(post("/api/sessao"), entrada("Diego Novo", "909"))).andExpect(status().isNoContent());
    }

    @Test
    void cadastroComPinJaPermiteEntrarComEle() throws Exception {
        JsonNode cadastrado = pedir(
                comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "Eva Nova", "empresa", "Unimed Exemplo", "setor", "Financeiro", "pin", "246")),
                status().isCreated());

        assertThat(cadastrado.get("temPin").asBoolean()).isTrue();
        mvc.perform(comJson(post("/api/sessao"), entrada("Eva Nova", "000")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(comJson(post("/api/sessao"), entrada("Eva Nova", "246"))).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "1234", "abc", "1 2", ""})
    void cadastroRecusaPinForaDoFormato(String pin) throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "Eva Nova", "empresa", "Unimed Exemplo", "setor", "Financeiro", "pin", pin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pin").exists());
    }

    @Test
    void cadastroSemPinSegueComoAntes() throws Exception {
        JsonNode cadastrado = pedir(
                comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "Eva Nova", "empresa", "Unimed Exemplo", "setor", "Financeiro")),
                status().isCreated());

        assertThat(cadastrado.get("temPin").asBoolean()).isFalse();
    }

    @Test
    void cadastroRecusaNomeRepetidoNoMesmoSetor() throws Exception {
        String setor = jdbc.queryForObject(
                "SELECT s.nome FROM setor s JOIN colaborador c ON c.setor_id = s.id WHERE c.nome = ?",
                String.class,
                "Ana Exemplo");

        mvc.perform(comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "ana exemplo", "empresa", "unimed exemplo", "setor", setor)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cadastroEPinExigemAdmin() throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/colaboradores").cookie(entrarComo("Ana Exemplo")),
                        Map.of("nome", "X", "empresa", "Y", "setor", "Z")))
                .andExpect(status().isUnauthorized());
    }
}

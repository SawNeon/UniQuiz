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

    private void definirPin(String nome, String pin) throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/colaboradores/{id}/redefinir-pin", idDoColaborador(nome))
                                .cookie(entrarComoAdmin()),
                        Map.of("pin", pin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void entraComOPinCadastradoEOBancoGuardaSoOHash() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", PIN_DE_TESTE)))
                .andExpect(status().isNoContent());

        String hash =
                jdbc.queryForObject("SELECT pin_hash FROM colaborador WHERE nome = ?", String.class, "Ana Exemplo");
        assertThat(hash).isNotBlank().doesNotContain(PIN_DE_TESTE);
    }

    @Test
    void primeiroAcessoNaoCriaPin() throws Exception {
        jdbc.update("UPDATE colaborador SET pin_hash = NULL WHERE nome = ?", "Ana Exemplo");

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "482")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value("O seu PIN ainda não foi cadastrado. Procure o administrador do UniQuiz."));

        assertThat(jdbc.queryForObject("SELECT pin_hash FROM colaborador WHERE nome = ?", String.class, "Ana Exemplo"))
                .isNull();
    }

    @Test
    void pinErradoNaoEntra() throws Exception {
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "111")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("PIN incorreto."));
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", PIN_DE_TESTE)))
                .andExpect(status().isNoContent());
    }

    @Test
    void listaDaEntradaDizSeAPessoaJaTemPin() throws Exception {
        long setor = jdbc.queryForObject("SELECT setor_id FROM colaborador WHERE nome = ?", Long.class, "Ana Exemplo");
        mvc.perform(get("/api/setores/{id}/colaboradores", setor).param("busca", "ana"))
                .andExpect(jsonPath("$[?(@.nome == 'Ana Exemplo')].temPin").value(true));

        jdbc.update("UPDATE colaborador SET pin_hash = NULL WHERE nome = ?", "Ana Exemplo");

        mvc.perform(get("/api/setores/{id}/colaboradores", setor).param("busca", "ana"))
                .andExpect(jsonPath("$[?(@.nome == 'Ana Exemplo')].temPin").value(false));
    }

    @Test
    void bloqueiaDepoisDeCincoPinsErradosEOAdminDefineOutro() throws Exception {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "000")))
                    .andExpect(status().isUnprocessableEntity());
        }

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", PIN_DE_TESTE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("bloqueado")));

        definirPin("Ana Exemplo", "777");

        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", "777"))).andExpect(status().isNoContent());
        mvc.perform(comJson(post("/api/sessao"), entrada("Ana Exemplo", PIN_DE_TESTE)))
                .andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "1234", "abc", "1 2", ""})
    void adminSoDefinePinDeTresNumeros(String pin) throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/colaboradores/{id}/redefinir-pin", idDoColaborador("Ana Exemplo"))
                                .cookie(entrarComoAdmin()),
                        Map.of("pin", pin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pin").exists());
    }

    @Test
    void definirPinDeQuemNaoExisteDa404() throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/colaboradores/{id}/redefinir-pin", 999_999)
                                .cookie(entrarComoAdmin()),
                        Map.of("pin", "321")))
                .andExpect(status().isNotFound());
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
    void adminCadastraColaboradorComPinCriandoOSetorQueFalta() throws Exception {
        JsonNode cadastrado = pedir(
                comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "Diego Novo", "empresa", "Unimed Exemplo", "setor", "Financeiro", "pin", "909")),
                status().isCreated());

        assertThat(cadastrado.get("temPin").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa WHERE nome = ?", Integer.class, "Unimed Exemplo"))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM setor WHERE nome = ?", Integer.class, "Financeiro"))
                .isEqualTo(1);
        mvc.perform(comJson(post("/api/sessao"), entrada("Diego Novo", "000")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(comJson(post("/api/sessao"), entrada("Diego Novo", "909"))).andExpect(status().isNoContent());
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
    void cadastroExigePin() throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "Eva Nova", "empresa", "Unimed Exemplo", "setor", "Financeiro")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pin").value("Informe o PIN de 3 números."));
    }

    @Test
    void cadastroRecusaNomeRepetidoNoMesmoSetor() throws Exception {
        String setor = jdbc.queryForObject(
                "SELECT s.nome FROM setor s JOIN colaborador c ON c.setor_id = s.id WHERE c.nome = ?",
                String.class,
                "Ana Exemplo");

        mvc.perform(comJson(
                        post("/api/admin/colaboradores").cookie(entrarComoAdmin()),
                        Map.of("nome", "ana exemplo", "empresa", "unimed exemplo", "setor", setor, "pin", "555")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cadastroEPinExigemAdmin() throws Exception {
        Cookie colaborador = entrarComo("Ana Exemplo");
        mvc.perform(comJson(
                        post("/api/admin/colaboradores").cookie(colaborador),
                        Map.of("nome", "X", "empresa", "Y", "setor", "Z", "pin", "123")))
                .andExpect(status().isUnauthorized());
        mvc.perform(comJson(
                        post("/api/admin/colaboradores/{id}/redefinir-pin", idDoColaborador("Bruno Teste"))
                                .cookie(colaborador),
                        Map.of("pin", "123")))
                .andExpect(status().isUnauthorized());
    }
}

package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        properties = {
            "spring.flyway.locations=classpath:db/migration,classpath:db/dev",
            "uniquiz.admin-inicial.usuario=admin",
            "uniquiz.admin-inicial.senha=senha-de-teste-longa"
        })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class FluxoDoColaboradorTest {

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> BANCO =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void rotasProtegidasExigemSessao() throws Exception {
        mvc.perform(get("/api/caminho")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/modulos")).andExpect(status().isUnauthorized());
    }

    @Test
    void sessaoDeColaboradorNaoAbreAAreaDoAdmin() throws Exception {
        Cookie sessao = entrarComoColaborador();

        mvc.perform(get("/api/admin/modulos").cookie(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void colaboradorFazUmaLicaoSemReceberOGabarito() throws Exception {
        Cookie sessao = entrarComoColaborador();

        JsonNode caminho = lerJson(mvc.perform(get("/api/caminho").cookie(sessao)));
        long licaoId = caminho.get(0).get("licoes").get(0).get("id").asLong();
        assertThat(caminho.get(0).get("licoes").get(0).get("situacao").asText()).isEqualTo("DISPONIVEL");

        String tentativaBruta = mvc.perform(post("/api/licoes/{id}/tentativas", licaoId).cookie(sessao))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(tentativaBruta).doesNotContain("\"correta\"", "\"motivo\"", "verdadeira", "motivoDoErro");

        JsonNode tentativa = json.readTree(tentativaBruta);
        long tentativaId = tentativa.get("tentativaId").asLong();
        for (JsonNode desafio : tentativa.get("desafios")) {
            mvc.perform(post("/api/tentativas/{id}/respostas", tentativaId)
                            .cookie(sessao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new EnvioDeTeste(
                                    desafio.get("id").asLong(), qualquerResposta(desafio)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.respostaCerta").exists());
        }

        mvc.perform(post("/api/tentativas/{id}/concluir", tentativaId).cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(tentativa.get("desafios").size()));

        mvc.perform(post("/api/tentativas/{id}/concluir", tentativaId).cookie(sessao))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adminEntraComUsuarioESenha() throws Exception {
        Cookie admin = mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"senha\":\"senha-de-teste-longa\"}"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");

        mvc.perform(get("/api/admin/modulos").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("LGPD na prática"));
    }

    @Test
    void certificadoSoSaiDepoisDeConcluirOModulo() throws Exception {
        Cookie sessao = entrarComoColaborador("Bruno Teste");
        JsonNode caminho = lerJson(mvc.perform(get("/api/caminho").cookie(sessao)));
        long moduloId = caminho.get(0).get("id").asLong();

        mvc.perform(post("/api/modulos/{id}/certificado", moduloId).cookie(sessao))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/publico/certificados/{codigo}", "AAAA-BBBB-CCCC"))
                .andExpect(status().isNotFound());
    }

    record EnvioDeTeste(long desafioId, Object resposta) {}

    private Object qualquerResposta(JsonNode desafio) {
        JsonNode conteudo = desafio.get("conteudo");
        return switch (desafio.get("tipo").asText()) {
            case "MULTIPLA_ESCOLHA" ->
                Map.of(
                        "alternativaId", conteudo.get("alternativas").get(0).get("id").asText());
            case "VERDADEIRO_OU_FALSO" -> Map.of("valor", true);
            case "COMPLETAR_CLICANDO" -> {
                Map<String, String> lacunas = new HashMap<>();
                conteudo.get("lacunas")
                        .forEach(lacuna -> lacunas.put(
                                lacuna.get("id").asText(),
                                lacuna.get("palavras").get(0).asText()));
                yield Map.of("lacunas", lacunas);
            }
            default -> throw new IllegalStateException("Tipo inesperado");
        };
    }

    private Cookie entrarComoColaborador() throws Exception {
        return entrarComoColaborador("Ana Exemplo");
    }

    private Cookie entrarComoColaborador(String nome) throws Exception {
        long empresaId = lerJson(mvc.perform(get("/api/empresas"))).get(0).get("id").asLong();
        JsonNode setores = lerJson(mvc.perform(get("/api/empresas/{id}/setores", empresaId)));
        for (JsonNode setor : setores) {
            for (JsonNode colaborador :
                    lerJson(mvc.perform(get("/api/setores/{id}/colaboradores", setor.get("id").asLong())))) {
                if (colaborador.get("nome").asText().equals(nome)) {
                    return mvc.perform(post("/api/sessao")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"colaboradorId\":" + colaborador.get("id").asLong() + "}"))
                            .andExpect(status().isNoContent())
                            .andReturn()
                            .getResponse()
                            .getCookie("uniquiz_sessao");
                }
            }
        }
        throw new IllegalStateException("Colaborador de exemplo não encontrado: " + nome);
    }

    private JsonNode lerJson(ResultActions acao) throws Exception {
        return json.readTree(acao.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}

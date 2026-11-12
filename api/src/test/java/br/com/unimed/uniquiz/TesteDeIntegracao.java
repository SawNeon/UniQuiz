package br.com.unimed.uniquiz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

@SpringBootTest(
        properties = {
            "spring.flyway.locations=classpath:db/migration,classpath:db/dev",
            "uniquiz.admin-inicial.usuario=admin",
            "uniquiz.admin-inicial.senha=" + TesteDeIntegracao.SENHA_DO_ADMIN,
            "uniquiz.limite-requisicoes.geral-por-minuto=100000",
            "uniquiz.limite-requisicoes.login-admin-por-minuto=100000"
        })
@AutoConfigureMockMvc
@EnabledIf(value = "dockerDisponivel", disabledReason = "Precisa do Docker para subir o SQL Server")
public abstract class TesteDeIntegracao {

    static final String PIN_DE_TESTE = "123";
    static final String SENHA_DO_ADMIN = "senha-de-teste-longa";
    static final String LICAO_DE_LGPD = "O básico da LGPD";
    static final String LICAO_DE_IA = "IA no trabalho";

    @ServiceConnection
    static final MSSQLServerContainer<?> BANCO =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    static {
        if (dockerDisponivel()) {
            BANCO.start();
        }
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    static boolean dockerDisponivel() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException erro) {
            return false;
        }
    }

    @BeforeEach
    void voltarAoConteudoDeExemplo() {
        jdbc.execute(
                """
                DELETE FROM resposta;
                DELETE FROM evento_pontos;
                DELETE FROM certificado;
                DELETE FROM desafio_da_tentativa;
                DELETE FROM tentativa;
                DELETE FROM colaborador_trofeu;
                DELETE FROM sequencia_diaria;
                DELETE FROM sessao;
                DELETE FROM desafio WHERE licao_id IN (
                    SELECT l.id FROM licao l JOIN modulo m ON m.id = l.modulo_id
                    WHERE m.titulo NOT IN (N'LGPD na prática', N'IA com responsabilidade'));
                DELETE FROM licao WHERE modulo_id IN (
                    SELECT id FROM modulo WHERE titulo NOT IN (N'LGPD na prática', N'IA com responsabilidade'));
                DELETE FROM modulo WHERE titulo NOT IN (N'LGPD na prática', N'IA com responsabilidade');
                UPDATE licao SET
                    qtd_facil = (SELECT COUNT(*) FROM desafio d WHERE d.licao_id = licao.id
                                 AND d.status = 'PUBLICADO' AND d.dificuldade = 'FACIL'),
                    qtd_medio = (SELECT COUNT(*) FROM desafio d WHERE d.licao_id = licao.id
                                 AND d.status = 'PUBLICADO' AND d.dificuldade = 'MEDIO'),
                    qtd_dificil = (SELECT COUNT(*) FROM desafio d WHERE d.licao_id = licao.id
                                   AND d.status = 'PUBLICADO' AND d.dificuldade = 'DIFICIL');
                DELETE FROM colaborador WHERE id NOT IN (
                    SELECT MIN(id) FROM colaborador
                    WHERE nome IN (N'Ana Exemplo', N'Bruno Teste', N'Carla Demonstração')
                    GROUP BY nome);
                UPDATE colaborador SET ativo = 1, pin_hash = NULL, tentativas_falhas_pin = 0, pin_bloqueado_ate = NULL;
                DELETE FROM setor WHERE id NOT IN (SELECT setor_id FROM colaborador);
                DELETE FROM empresa WHERE id NOT IN (SELECT empresa_id FROM colaborador);
                DELETE FROM temporada WHERE nome <> N'Temporada de exemplo';
                UPDATE administrador SET tentativas_falhas = 0, bloqueado_ate = NULL;
                """);
    }

    protected long idDoColaborador(String nome) {
        return jdbc.queryForObject("SELECT id FROM colaborador WHERE nome = ?", Long.class, nome);
    }

    protected long idDaLicao(String titulo) {
        return jdbc.queryForObject("SELECT id FROM licao WHERE titulo = ?", Long.class, titulo);
    }

    protected long idDoModuloDaLicao(String tituloDaLicao) {
        return jdbc.queryForObject("SELECT modulo_id FROM licao WHERE titulo = ?", Long.class, tituloDaLicao);
    }

    protected Cookie entrarComo(String nome) throws Exception {
        return mvc.perform(comJson(
                        post("/api/sessao"), Map.of("colaboradorId", idDoColaborador(nome), "pin", PIN_DE_TESTE)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_sessao");
    }

    protected Cookie entrarComoAdmin() throws Exception {
        return mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", "admin", "senha", SENHA_DO_ADMIN))))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");
    }

    protected JsonNode pedir(MockHttpServletRequestBuilder pedido, ResultMatcher esperado) throws Exception {
        String corpo = mvc.perform(pedido)
                .andExpect(esperado)
                .andReturn()
                .getResponse()
                .getContentAsString();
        return corpo.isEmpty() ? json.nullNode() : json.readTree(corpo);
    }

    protected JsonNode ler(String caminho, Cookie sessao) throws Exception {
        return pedir(get(caminho).cookie(sessao), status().isOk());
    }

    protected MockHttpServletRequestBuilder comJson(MockHttpServletRequestBuilder pedido, Object corpo)
            throws Exception {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
    }

    protected Map<Long, Object> gabarito(String tituloDaLicao, boolean certo) throws Exception {
        Cookie admin = entrarComoAdmin();
        JsonNode desafios = ler("/api/admin/licoes/" + idDaLicao(tituloDaLicao) + "/desafios", admin);
        Map<Long, Object> respostas = new LinkedHashMap<>();
        for (JsonNode desafio : desafios) {
            respostas.put(desafio.get("id").asLong(), resposta(desafio, certo));
        }
        return respostas;
    }

    protected JsonNode iniciar(Cookie sessao, String tituloDaLicao) throws Exception {
        return pedir(
                post("/api/licoes/{id}/tentativas", idDaLicao(tituloDaLicao)).cookie(sessao), status().isCreated());
    }

    protected JsonNode responder(
            Cookie sessao, long tentativaId, long desafioId, Object resposta, ResultMatcher esperado) throws Exception {
        return pedir(
                comJson(
                        post("/api/tentativas/{id}/respostas", tentativaId).cookie(sessao),
                        Map.of("desafioId", desafioId, "resposta", resposta)),
                esperado);
    }

    protected JsonNode concluir(Cookie sessao, long tentativaId) throws Exception {
        return pedir(post("/api/tentativas/{id}/concluir", tentativaId).cookie(sessao), status().isOk());
    }

    protected JsonNode fazerLicao(Cookie sessao, String tituloDaLicao, boolean acertarTudo) throws Exception {
        Map<Long, Object> respostas = gabarito(tituloDaLicao, acertarTudo);
        JsonNode tentativa = iniciar(sessao, tituloDaLicao);
        long tentativaId = tentativa.get("tentativaId").asLong();
        for (JsonNode sorteado : tentativa.get("desafios")) {
            long desafioId = sorteado.get("id").asLong();
            responder(sessao, tentativaId, desafioId, respostas.get(desafioId), status().isOk());
        }
        return concluir(sessao, tentativaId);
    }

    private static Object resposta(JsonNode desafio, boolean certo) {
        JsonNode conteudo = desafio.get("conteudo");
        return switch (desafio.get("tipo").asText()) {
            case "MULTIPLA_ESCOLHA" -> {
                for (JsonNode alternativa : conteudo.get("alternativas")) {
                    if (alternativa.get("correta").asBoolean() == certo) {
                        yield Map.of("alternativaId", alternativa.get("id").asText());
                    }
                }
                throw new IllegalStateException("Alternativa não encontrada");
            }
            case "VERDADEIRO_OU_FALSO" ->
                Map.of("valor", conteudo.get("verdadeira").asBoolean() == certo);
            case "COMPLETAR_CLICANDO" -> {
                Map<String, String> lacunas = new HashMap<>();
                for (JsonNode lacuna : conteudo.get("lacunas")) {
                    for (JsonNode opcao : lacuna.get("banco")) {
                        if (opcao.get("correta").asBoolean() == certo) {
                            lacunas.put(
                                    lacuna.get("id").asText(),
                                    opcao.get("palavra").asText());
                            break;
                        }
                    }
                }
                yield Map.of("lacunas", lacunas);
            }
            default -> throw new IllegalStateException("Tipo inesperado: " + desafio.get("tipo"));
        };
    }
}

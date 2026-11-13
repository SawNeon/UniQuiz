package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class SessoesEAuditoriaTest extends TesteDeIntegracao {

    private static final String NAVEGADOR =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";

    private long ultimoRegistro() {
        return jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM registro_de_auditoria", Long.class);
    }

    private Map<String, Object> registroDepoisDe(long id, String acao) {
        return jdbc.queryForMap(
                "SELECT TOP 1 * FROM registro_de_auditoria WHERE id > ? AND acao = ? ORDER BY id DESC", id, acao);
    }

    private Cookie entrarComoAdminPeloNavegador() throws Exception {
        return mvc.perform(comJson(
                        post("/api/admin/login").header(HttpHeaders.USER_AGENT, NAVEGADOR),
                        Map.of("usuario", "admin", "senha", SENHA_DO_ADMIN)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");
    }

    @Test
    void cookieDoPainelSoVaiEmRequisicoesDoProprioSite() throws Exception {
        mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "admin", "senha", SENHA_DO_ADMIN)))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    void minhasSessoesMostramOAparelhoEPermitemEncerrar() throws Exception {
        Cookie atual = entrarComoAdminPeloNavegador();
        Cookie outra = entrarComoAdmin();

        JsonNode sessoes = ler("/api/admin/minha-conta/sessoes", atual);
        assertThat(sessoes).hasSize(2);
        JsonNode daqui = null;
        JsonNode dali = null;
        for (JsonNode sessao : sessoes) {
            if (sessao.get("atual").asBoolean()) {
                daqui = sessao;
            } else {
                dali = sessao;
            }
        }
        assertThat(daqui).isNotNull();
        assertThat(daqui.get("dispositivo").asText()).isEqualTo("Chrome no Windows");
        assertThat(daqui.get("id").asText()).hasSize(16);

        mvc.perform(delete("/api/admin/minha-conta/sessoes/{id}", dali.get("id").asText())
                        .cookie(atual))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/eu").cookie(outra)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/eu").cookie(atual)).andExpect(status().isOk());
    }

    @Test
    void sairDosOutrosAparelhosMantemOAtual() throws Exception {
        Cookie atual = entrarComoAdmin();
        Cookie outra = entrarComoAdmin();

        mvc.perform(post("/api/admin/minha-conta/sessoes/encerrar-outras").cookie(atual))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/eu").cookie(outra)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/eu").cookie(atual)).andExpect(status().isOk());
    }

    @Test
    void sessaoParadaHaMaisDeTrintaMinutosCai() throws Exception {
        Cookie sessao = entrarComoAdmin();

        jdbc.update("UPDATE sessao SET ultimo_uso_em = DATEADD(MINUTE, -31, SYSUTCDATETIME())"
                + " WHERE administrador_id IS NOT NULL");

        mvc.perform(get("/api/admin/eu").cookie(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void entradasNoPainelFicamRegistradasComOMotivoDaFalha() throws Exception {
        long antes = ultimoRegistro();

        mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "ninguem", "senha", "qualquer")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "admin", "senha", "errada")))
                .andExpect(status().isUnprocessableEntity());
        Cookie sessao = entrarComoAdmin();
        mvc.perform(delete("/api/admin/sessao").cookie(sessao)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForList(
                        "SELECT ator_tipo, ator_descricao, resultado, detalhe FROM registro_de_auditoria"
                                + " WHERE id > ? AND acao = 'ENTRADA_NO_PAINEL' ORDER BY id",
                        antes))
                .extracting(linha -> linha.get("ator_tipo") + "|" + linha.get("ator_descricao") + "|"
                        + linha.get("resultado") + "|" + linha.get("detalhe"))
                .containsExactly(
                        "ANONIMO|ninguem|FALHA|Usuário inexistente ou desativado",
                        "ADMINISTRADOR|null|FALHA|Senha incorreta",
                        "ADMINISTRADOR|null|SUCESSO|null");
        assertThat(registroDepoisDe(antes, "SAIDA_DO_PAINEL")).containsEntry("resultado", "SUCESSO");
    }

    @Test
    void entradaDoColaboradorRegistraPrimeiroAcessoEPinErrado() throws Exception {
        long antes = ultimoRegistro();
        long ana = idDoColaborador("Ana Exemplo");

        entrarComo("Ana Exemplo");
        mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", ana, "pin", "999")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(jdbc.queryForList(
                        "SELECT resultado, detalhe FROM registro_de_auditoria"
                                + " WHERE id > ? AND acao = 'ENTRADA_DO_COLABORADOR' AND ator_id = ? ORDER BY id",
                        antes,
                        ana))
                .extracting(linha -> linha.get("resultado") + "|" + linha.get("detalhe"))
                .containsExactly("SUCESSO|Primeiro acesso: PIN criado", "FALHA|PIN incorreto.");
    }

    @Test
    void auditorConsultaORegistroComFiltrosEAlertas() throws Exception {
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", "admin", "senha", "errada")));
        }
        Cookie admin = entrarComoAdmin();
        JsonNode criada = pedir(
                comJson(
                        post("/api/admin/usuarios").cookie(admin),
                        Map.of("nome", "Rui", "usuario", "rui.auditor", "perfil", "AUDITOR", "setores", Set.of())),
                status().isCreated());
        Cookie auditor = mvc.perform(comJson(
                        post("/api/admin/login"),
                        Map.of(
                                "usuario",
                                "rui.auditor",
                                "senha",
                                criada.get("senha").asText())))
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");
        mvc.perform(comJson(
                        post("/api/admin/minha-conta/senha").cookie(auditor),
                        Map.of("senhaAtual", criada.get("senha").asText(), "novaSenha", "frase longa do auditor")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/auditoria")
                        .param("acao", "ENTRADA_NO_PAINEL")
                        .param("resultado", "FALHA")
                        .param("busca", "Admin")
                        .cookie(auditor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.itens[0].acaoDescricao").value("Entrou no painel"))
                .andExpect(jsonPath("$.itens[0].atorNome").value("admin"));

        mvc.perform(get("/api/admin/auditoria/alertas").cookie(auditor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.falhasPorPessoa[?(@.quem == 'admin')].falhas")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThanOrEqualTo(3))));

        mvc.perform(get("/api/admin/auditoria")
                        .param("de", "2026-12-10")
                        .param("ate", "2026-12-01")
                        .cookie(auditor))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void gestorNaoVeAuditoria() throws Exception {
        long setor = jdbc.queryForObject("SELECT MIN(id) FROM setor", Long.class);
        Cookie admin = entrarComoAdmin();
        JsonNode criada = pedir(
                comJson(
                        post("/api/admin/usuarios").cookie(admin),
                        Map.of("nome", "Gil", "usuario", "gil.gestor", "perfil", "GESTOR", "setores", Set.of(setor))),
                status().isCreated());
        Cookie gestor = mvc.perform(comJson(
                        post("/api/admin/login"),
                        Map.of(
                                "usuario",
                                "gil.gestor",
                                "senha",
                                criada.get("senha").asText())))
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");
        mvc.perform(comJson(
                        post("/api/admin/minha-conta/senha").cookie(gestor),
                        Map.of("senhaAtual", criada.get("senha").asText(), "novaSenha", "frase longa do gestor")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/auditoria").cookie(gestor)).andExpect(status().isForbidden());
    }
}

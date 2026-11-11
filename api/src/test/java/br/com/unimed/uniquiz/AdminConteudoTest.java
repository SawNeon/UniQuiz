package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminConteudoTest extends TesteDeIntegracao {

    private Cookie admin;

    @BeforeEach
    void entrar() throws Exception {
        admin = entrarComoAdmin();
    }

    @Test
    void montaEPublicaUmModuloDoComecoAoFim() throws Exception {
        long moduloId = criarModulo("Golpes digitais", "# Golpes\n\nDesconfie de **urgência**.");
        Cookie ana = entrarComo("Ana Exemplo");

        assertThat(ler("/api/caminho", ana).findValuesAsText("titulo")).doesNotContain("Golpes digitais");
        pedir(get("/api/modulos/{id}/estudo", moduloId).cookie(ana), status().isNotFound());
        pedir(
                post("/api/admin/modulos/{id}/status", moduloId)
                        .cookie(admin)
                        .contentType("application/json")
                        .content("{\"status\":\"PUBLICADO\"}"),
                status().isUnprocessableEntity());

        long licaoId = criarLicao(moduloId, "Mensagens suspeitas");
        for (int numero = 1; numero <= 4; numero++) {
            publicar("desafios", criarDesafio(licaoId, verdadeiroOuFalso("Afirmação " + numero)));
        }
        pedir(
                comJson(post("/api/admin/licoes/{id}/status", licaoId).cookie(admin), Map.of("status", "PUBLICADO")),
                status().isUnprocessableEntity());

        publicar("desafios", criarDesafio(licaoId, verdadeiroOuFalso("Afirmação 5")));
        publicar("licoes", licaoId);
        publicar("modulos", moduloId);

        assertThat(ler("/api/caminho", ana).findValuesAsText("titulo"))
                .contains("Golpes digitais", "Mensagens suspeitas");
        assertThat(ler("/api/modulos/" + moduloId + "/estudo", ana)
                        .get("conteudo")
                        .asText())
                .contains("urgência");
    }

    @Test
    void recusaConteudoDeDesafioInvalido() throws Exception {
        long licaoId = criarLicao(criarModulo("Rascunho", ""), "Lição");

        Map<String, Object> duasCertas = desafio(
                "MULTIPLA_ESCOLHA",
                Map.of("alternativas", List.of(alternativa("a", true, ""), alternativa("b", true, ""))));
        pedir(
                comJson(post("/api/admin/licoes/{id}/desafios", licaoId).cookie(admin), duasCertas),
                status().isUnprocessableEntity());

        Map<String, Object> erradaSemMotivo = desafio(
                "MULTIPLA_ESCOLHA",
                Map.of("alternativas", List.of(alternativa("a", true, ""), alternativa("b", false, ""))));
        mvc.perform(comJson(post("/api/admin/licoes/{id}/desafios", licaoId).cookie(admin), erradaSemMotivo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("está errada")));

        Map<String, Object> lacunaSemBanco = desafio(
                "COMPLETAR_CLICANDO", Map.of("texto", "Use ferramentas {{1}} e {{2}}.", "lacunas", List.of(lacuna(1))));
        pedir(
                comJson(post("/api/admin/licoes/{id}/desafios", licaoId).cookie(admin), lacunaSemBanco),
                status().isUnprocessableEntity());

        pedir(
                comJson(
                        post("/api/admin/licoes/{id}/desafios", licaoId).cookie(admin),
                        Map.of("tipo", "MULTIPLA_ESCOLHA")),
                status().isBadRequest());
    }

    @Test
    void aceitaOsTresTiposDeDesafio() throws Exception {
        long licaoId = criarLicao(criarModulo("Tipos", ""), "Lição");

        criarDesafio(
                licaoId,
                desafio(
                        "MULTIPLA_ESCOLHA",
                        Map.of(
                                "alternativas",
                                List.of(alternativa("a", true, ""), alternativa("b", false, "Porque não.")))));
        criarDesafio(licaoId, verdadeiroOuFalso("Senha longa é melhor."));
        criarDesafio(
                licaoId,
                desafio(
                        "COMPLETAR_CLICANDO",
                        Map.of("texto", "Use ferramentas {{1}}.", "lacunas", List.of(lacuna(1)))));

        assertThat(ler("/api/admin/licoes/" + licaoId + "/desafios", admin)).hasSize(3);
    }

    @Test
    void editarDesafioPublicadoCriaNovaVersaoEDuplicarCriaRascunho() throws Exception {
        long licaoId = criarLicao(criarModulo("Versões", ""), "Lição");
        long desafioId = criarDesafio(licaoId, verdadeiroOuFalso("Original"));
        publicar("desafios", desafioId);

        JsonNode editado = pedir(
                comJson(put("/api/admin/desafios/{id}", desafioId).cookie(admin), verdadeiroOuFalso("Editado")),
                status().isOk());
        assertThat(editado.get("versao").asInt()).isEqualTo(2);
        assertThat(editado.get("conteudo").get("afirmacao").asText()).isEqualTo("Editado");

        JsonNode copia =
                pedir(post("/api/admin/desafios/{id}/duplicar", desafioId).cookie(admin), status().isCreated());
        assertThat(copia.get("status").asText()).isEqualTo("RASCUNHO");
        assertThat(copia.get("versao").asInt()).isEqualTo(1);
        assertThat(copia.get("id").asLong()).isNotEqualTo(desafioId);
    }

    @Test
    void reordenaAsLicoesDoModulo() throws Exception {
        long moduloId = criarModulo("Ordem", "");
        long primeira = criarLicao(moduloId, "Primeira");
        long segunda = criarLicao(moduloId, "Segunda");

        pedir(
                comJson(
                        patch("/api/admin/modulos/{id}/licoes/ordem", moduloId).cookie(admin),
                        Map.of("ids", List.of(primeira))),
                status().isUnprocessableEntity());
        pedir(
                comJson(
                        patch("/api/admin/modulos/{id}/licoes/ordem", moduloId).cookie(admin),
                        Map.of("ids", List.of(segunda, primeira))),
                status().isNoContent());

        List<String> titulos = new ArrayList<>();
        for (JsonNode modulo : ler("/api/admin/modulos", admin)) {
            if (modulo.get("id").asLong() == moduloId) {
                modulo.get("licoes")
                        .forEach(licao -> titulos.add(licao.get("titulo").asText()));
            }
        }
        assertThat(titulos).containsExactly("Segunda", "Primeira");

        pedir(
                comJson(put("/api/admin/licoes/{id}", primeira).cookie(admin), Map.of("titulo", "Renomeada")),
                status().isOk());
        assertThat(ler("/api/admin/modulos", admin).findValuesAsText("titulo")).contains("Renomeada");
    }

    @Test
    void atualizaOModuloEOMaterialDeEstudo() throws Exception {
        long moduloId = criarModulo("Antigo", "");

        JsonNode atualizado = pedir(
                comJson(
                        put("/api/admin/modulos/{id}", moduloId).cookie(admin),
                        Map.of("titulo", "Novo", "descricao", "Descrição", "materialEstudo", "Texto")),
                status().isOk());

        assertThat(atualizado.get("titulo").asText()).isEqualTo("Novo");
        assertThat(atualizado.get("materialEstudo").asText()).isEqualTo("Texto");
    }

    private long criarModulo(String titulo, String material) throws Exception {
        return pedir(
                        comJson(
                                post("/api/admin/modulos").cookie(admin),
                                Map.of("titulo", titulo, "descricao", "Teste", "materialEstudo", material)),
                        status().isCreated())
                .get("id")
                .asLong();
    }

    private long criarLicao(long moduloId, String titulo) throws Exception {
        return pedir(
                        comJson(
                                post("/api/admin/modulos/{id}/licoes", moduloId).cookie(admin),
                                Map.of("titulo", titulo)),
                        status().isCreated())
                .get("id")
                .asLong();
    }

    private long criarDesafio(long licaoId, Map<String, Object> dados) throws Exception {
        return pedir(
                        comJson(post("/api/admin/licoes/{id}/desafios", licaoId).cookie(admin), dados),
                        status().isCreated())
                .get("id")
                .asLong();
    }

    private void publicar(String recurso, long id) throws Exception {
        pedir(
                comJson(
                        post("/api/admin/" + recurso + "/{id}/status", id).cookie(admin),
                        Map.of("status", "PUBLICADO")),
                status().isNoContent());
    }

    private static Map<String, Object> verdadeiroOuFalso(String afirmacao) {
        return desafio(
                "VERDADEIRO_OU_FALSO",
                Map.of("afirmacao", afirmacao, "verdadeira", true, "motivoDoErro", "Explicação do erro."));
    }

    private static Map<String, Object> desafio(String tipo, Map<String, Object> conteudo) {
        return Map.of(
                "tipo",
                tipo,
                "enunciado",
                "Enunciado",
                "dificuldade",
                "FACIL",
                "pontos",
                10,
                "explicacao",
                "Explicação geral.",
                "conteudo",
                conteudo);
    }

    private static Map<String, Object> alternativa(String id, boolean correta, String motivo) {
        return Map.of("id", id, "texto", "Alternativa " + id, "correta", correta, "motivo", motivo);
    }

    private static Map<String, Object> lacuna(int id) {
        return Map.of(
                "id",
                id,
                "banco",
                List.of(
                        Map.of("palavra", "aprovadas", "correta", true, "motivo", ""),
                        Map.of("palavra", "gratuitas", "correta", false, "motivo", "Gratuita não é segura.")));
    }
}

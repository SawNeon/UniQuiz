package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SorteioTest extends TesteDeIntegracao {

    private Cookie admin;
    private Cookie ana;

    @BeforeEach
    void entrar() throws Exception {
        admin = entrarComoAdmin();
        ana = entrarComo("Ana Exemplo");
    }

    @Test
    void sorteiaSoAQuantidadePedidaDeCadaNivel() throws Exception {
        definirSorteio(LICAO_DE_LGPD, 1, 1, 1);

        JsonNode desafios = iniciar(ana, LICAO_DE_LGPD).get("desafios");

        assertThat(desafios).hasSize(3);
        assertThat(niveis(desafios)).containsExactlyInAnyOrder("FACIL", "MEDIO", "DIFICIL");
    }

    @Test
    void cadaTentativaTrazDesafiosEOrdemDiferentes() throws Exception {
        definirSorteio(LICAO_DE_LGPD, 1, 1, 1);

        Set<List<Long>> sequencias = new HashSet<>();
        for (int tentativa = 0; tentativa < 15; tentativa++) {
            sequencias.add(ids(iniciar(ana, LICAO_DE_LGPD).get("desafios")));
        }

        assertThat(sequencias.size()).isGreaterThan(1);
    }

    @Test
    void naoAceitaRespostaDeDesafioQueNaoFoiSorteado() throws Exception {
        definirSorteio(LICAO_DE_LGPD, 1, 0, 0);
        Map<Long, Object> certas = gabarito(LICAO_DE_LGPD, true);

        JsonNode tentativa = iniciar(ana, LICAO_DE_LGPD);
        long tentativaId = tentativa.get("tentativaId").asLong();
        long sorteado = tentativa.get("desafios").get(0).get("id").asLong();
        long deFora = certas.keySet().stream()
                .filter(id -> id != sorteado)
                .findFirst()
                .orElseThrow();

        responder(ana, tentativaId, deFora, certas.get(deFora), status().isNotFound());
        responder(ana, tentativaId, sorteado, certas.get(sorteado), status().isOk());
        assertThat(concluir(ana, tentativaId).get("total").asInt()).isEqualTo(1);
    }

    @Test
    void licaoComBancoMaiorQueOSorteioConcluiComOTotalSorteado() throws Exception {
        definirSorteio(LICAO_DE_LGPD, 1, 1, 0);

        JsonNode resultado = fazerLicao(ana, LICAO_DE_LGPD, true);

        assertThat(resultado.get("total").asInt()).isEqualTo(2);
        assertThat(resultado.get("acertos").asInt()).isEqualTo(2);
        assertThat(resultado.get("perfeita").asBoolean()).isTrue();
    }

    @Test
    void naoPublicaLicaoSemQuantidadeOuComBancoPequenoDemais() throws Exception {
        long moduloId = criarModulo("Sorteio");
        long licaoId = criarLicao(moduloId, "Sem quantidade", Map.of("facil", 0, "medio", 0, "dificil", 0));
        mudarStatus("licoes", licaoId, status().isUnprocessableEntity());

        long exigente = criarLicao(moduloId, "Exigente", Map.of("facil", 2, "medio", 1, "dificil", 0));
        publicarDesafios(exigente, "FACIL", 3);
        mvc.perform(comJson(
                        post("/api/admin/licoes/{id}/status", exigente).cookie(admin), Map.of("status", "PUBLICADO")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("médio")));

        publicarDesafios(exigente, "MEDIO", 1);
        mudarStatus("licoes", exigente, status().isNoContent());
    }

    @Test
    void bancoPodeTerMaisDesafiosDoQueOsSorteados() throws Exception {
        long moduloId = criarModulo("Banco grande");
        long licaoId = criarLicao(moduloId, "Banco grande", Map.of("facil", 2, "medio", 0, "dificil", 0));
        publicarDesafios(licaoId, "FACIL", 12);

        mudarStatus("licoes", licaoId, status().isNoContent());

        JsonNode licao = licaoDoAdmin(licaoId);
        assertThat(licao.get("publicadosPorNivel").get("facil").asInt()).isEqualTo(12);
        assertThat(licao.get("quantidades").get("facil").asInt()).isEqualTo(2);
    }

    @Test
    void naoTiraDoBancoUmDesafioQueFariaFaltarParaOSorteio() throws Exception {
        long moduloId = criarModulo("Tirar do banco");
        long licaoId = criarLicao(moduloId, "Tirar do banco", Map.of("facil", 2, "medio", 0, "dificil", 0));
        List<Long> desafios = publicarDesafios(licaoId, "FACIL", 2);
        mudarStatus("licoes", licaoId, status().isNoContent());

        pedir(
                comJson(
                        post("/api/admin/desafios/{id}/status", desafios.getFirst())
                                .cookie(admin),
                        Map.of("status", "RASCUNHO")),
                status().isUnprocessableEntity());

        publicarDesafios(licaoId, "FACIL", 1);
        pedir(
                comJson(
                        post("/api/admin/desafios/{id}/status", desafios.getFirst())
                                .cookie(admin),
                        Map.of("status", "RASCUNHO")),
                status().isNoContent());
    }

    @Test
    void mudarSoOTituloNaoApagaAsQuantidades() throws Exception {
        long moduloId = criarModulo("Título");
        long licaoId = criarLicao(moduloId, "Antes", Map.of("facil", 4, "medio", 3, "dificil", 2));

        pedir(
                comJson(put("/api/admin/licoes/{id}", licaoId).cookie(admin), Map.of("titulo", "Depois")),
                status().isOk());

        JsonNode licao = licaoDoAdmin(licaoId);
        assertThat(licao.get("titulo").asText()).isEqualTo("Depois");
        assertThat(licao.get("quantidades").get("facil").asInt()).isEqualTo(4);
        assertThat(licao.get("quantidades").get("medio").asInt()).isEqualTo(3);
        assertThat(licao.get("quantidades").get("dificil").asInt()).isEqualTo(2);
    }

    @Test
    void recusaQuantidadeForaDoLimite() throws Exception {
        long moduloId = criarModulo("Limite");

        pedir(
                comJson(
                        post("/api/admin/modulos/{id}/licoes", moduloId).cookie(admin),
                        Map.of("titulo", "Muito", "quantidades", Map.of("facil", 31, "medio", 0, "dificil", 0))),
                status().isBadRequest());
        pedir(
                comJson(
                        post("/api/admin/modulos/{id}/licoes", moduloId).cookie(admin),
                        Map.of("titulo", "Negativo", "quantidades", Map.of("facil", -1, "medio", 0, "dificil", 0))),
                status().isBadRequest());
    }

    @Test
    void naoDiminuiOBancoAbaixoDaQuantidadeDeUmaLicaoPublicada() throws Exception {
        long moduloId = criarModulo("Aumentar");
        long licaoId = criarLicao(moduloId, "Aumentar", Map.of("facil", 2, "medio", 0, "dificil", 0));
        publicarDesafios(licaoId, "FACIL", 2);
        mudarStatus("licoes", licaoId, status().isNoContent());

        pedir(
                comJson(
                        put("/api/admin/licoes/{id}", licaoId).cookie(admin),
                        Map.of("titulo", "Aumentar", "quantidades", Map.of("facil", 3, "medio", 0, "dificil", 0))),
                status().isUnprocessableEntity());
        pedir(
                comJson(
                        put("/api/admin/licoes/{id}", licaoId).cookie(admin),
                        Map.of("titulo", "Aumentar", "quantidades", Map.of("facil", 1, "medio", 0, "dificil", 0))),
                status().isOk());
    }

    private void definirSorteio(String tituloDaLicao, int facil, int medio, int dificil) throws Exception {
        pedir(
                comJson(
                        put("/api/admin/licoes/{id}", idDaLicao(tituloDaLicao)).cookie(admin),
                        Map.of(
                                "titulo",
                                tituloDaLicao,
                                "quantidades",
                                Map.of("facil", facil, "medio", medio, "dificil", dificil))),
                status().isOk());
    }

    private long criarModulo(String titulo) throws Exception {
        return pedir(
                        comJson(
                                post("/api/admin/modulos").cookie(admin),
                                Map.of("titulo", titulo, "descricao", "Teste", "materialEstudo", "")),
                        status().isCreated())
                .get("id")
                .asLong();
    }

    private long criarLicao(long moduloId, String titulo, Map<String, Integer> quantidades) throws Exception {
        return pedir(
                        comJson(
                                post("/api/admin/modulos/{id}/licoes", moduloId).cookie(admin),
                                Map.of("titulo", titulo, "quantidades", quantidades)),
                        status().isCreated())
                .get("id")
                .asLong();
    }

    private List<Long> publicarDesafios(long licaoId, String dificuldade, int quantidade) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            long id = pedir(
                            comJson(
                                    post("/api/admin/licoes/{id}/desafios", licaoId)
                                            .cookie(admin),
                                    verdadeiroOuFalso(dificuldade, "Afirmação " + numero)),
                            status().isCreated())
                    .get("id")
                    .asLong();
            mudarStatus("desafios", id, status().isNoContent());
            ids.add(id);
        }
        return ids;
    }

    private void mudarStatus(String recurso, long id, org.springframework.test.web.servlet.ResultMatcher esperado)
            throws Exception {
        pedir(
                comJson(
                        post("/api/admin/" + recurso + "/{id}/status", id).cookie(admin),
                        Map.of("status", "PUBLICADO")),
                esperado);
    }

    private JsonNode licaoDoAdmin(long licaoId) throws Exception {
        for (JsonNode modulo : ler("/api/admin/modulos", admin)) {
            for (JsonNode licao : modulo.get("licoes")) {
                if (licao.get("id").asLong() == licaoId) {
                    return licao;
                }
            }
        }
        throw new IllegalStateException("Lição não encontrada: " + licaoId);
    }

    private static Map<String, Object> verdadeiroOuFalso(String dificuldade, String afirmacao) {
        return Map.of(
                "tipo",
                "VERDADEIRO_OU_FALSO",
                "enunciado",
                "Enunciado",
                "dificuldade",
                dificuldade,
                "pontos",
                10,
                "explicacao",
                "Explicação geral.",
                "conteudo",
                Map.of("afirmacao", afirmacao, "verdadeira", true, "motivoDoErro", "Explicação do erro."));
    }

    private static List<String> niveis(JsonNode desafios) {
        List<String> niveis = new ArrayList<>();
        desafios.forEach(desafio -> niveis.add(desafio.get("dificuldade").asText()));
        return niveis;
    }

    private static List<Long> ids(JsonNode desafios) {
        List<Long> ids = new ArrayList<>();
        desafios.forEach(desafio -> ids.add(desafio.get("id").asLong()));
        return ids;
    }
}

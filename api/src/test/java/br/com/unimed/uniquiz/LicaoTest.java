package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LicaoTest extends TesteDeIntegracao {

    @Test
    void desafiosChegamSemGabarito() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");

        String tentativa = mvc.perform(post("/api/licoes/{id}/tentativas", idDaLicao(LICAO_DE_LGPD))
                        .cookie(sessao))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(tentativa)
                .contains("\"desafios\"")
                .doesNotContain("\"correta\"", "\"motivo\"", "verdadeira", "motivoDoErro", "explicacao");
    }

    @Test
    void licaoPerfeitaSomaPontosBonusETrofeus() throws Exception {
        JsonNode resultado = fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);

        assertThat(resultado.get("perfeita").asBoolean()).isTrue();
        assertThat(resultado.get("acertos").asInt()).isEqualTo(5);
        assertThat(resultado.get("pontosGanhos").asInt()).isEqualTo(70 + 15 + 20 + 10);
        assertThat(motivos(resultado)).containsExactly("ACERTOS", "LICAO_PERFEITA", "PRIMEIRA_LICAO_DO_DIA");
        assertThat(codigos(resultado.get("trofeusNovos")))
                .containsExactlyInAnyOrder("PRIMEIRO_PASSO", "LICAO_PERFEITA", "MODULO_CONCLUIDO");
        assertThat(resultado.get("moduloConcluido").get("titulo").asText()).isEqualTo("LGPD na prática");
        assertThat(resultado.get("diasSeguidos").asInt()).isEqualTo(1);
    }

    @Test
    void refazerValeMetadeUmaVezEDepoisNaoPontua() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        fazerLicao(sessao, LICAO_DE_LGPD, true);

        JsonNode segunda = iniciar(sessao, LICAO_DE_LGPD);
        assertThat(segunda.get("valePontosIntegrais").asBoolean()).isFalse();
        JsonNode segundaVez = fazerLicao(sessao, LICAO_DE_LGPD, true);
        assertThat(segundaVez.get("pontosGanhos").asInt()).isEqualTo(34 + 6 + 10);
        assertThat(segundaVez.get("trofeusNovos")).isEmpty();

        JsonNode terceiraVez = fazerLicao(sessao, LICAO_DE_LGPD, true);
        assertThat(terceiraVez.get("pontosGanhos").asInt()).isZero();
    }

    @Test
    void errosVoltamParaSegundaChanceSemPontos() throws Exception {
        Cookie sessao = entrarComo("Bruno Teste");
        Map<Long, Object> erradas = gabarito(LICAO_DE_LGPD, false);
        Map<Long, Object> certas = gabarito(LICAO_DE_LGPD, true);
        long tentativaId = iniciar(sessao, LICAO_DE_LGPD).get("tentativaId").asLong();
        List<Long> desafios = new ArrayList<>(erradas.keySet());

        JsonNode primeiraCorrecao =
                responder(sessao, tentativaId, desafios.getFirst(), erradas.get(desafios.getFirst()), status().isOk());
        assertThat(primeiraCorrecao.get("correta").asBoolean()).isFalse();
        assertThat(primeiraCorrecao.get("motivoDoErro").asText()).isNotBlank();
        assertThat(primeiraCorrecao.get("respostaCerta").asText()).isNotBlank();
        assertThat(primeiraCorrecao.get("desafiosParaSegundaChance")).isEmpty();

        responder(
                sessao,
                tentativaId,
                desafios.getFirst(),
                certas.get(desafios.getFirst()),
                status().isUnprocessableEntity());

        JsonNode ultima = null;
        for (Long desafio : desafios.subList(1, desafios.size())) {
            ultima = responder(sessao, tentativaId, desafio, erradas.get(desafio), status().isOk());
        }
        assertThat(ultima.get("desafiosParaSegundaChance")).hasSize(5);

        JsonNode segundaChance =
                responder(sessao, tentativaId, desafios.getFirst(), certas.get(desafios.getFirst()), status().isOk());
        assertThat(segundaChance.get("correta").asBoolean()).isTrue();
        assertThat(segundaChance.get("segundaChance").asBoolean()).isTrue();
        assertThat(segundaChance.get("pontos").asInt()).isZero();
        assertThat(segundaChance.get("desafiosParaSegundaChance")).hasSize(4);

        responder(
                sessao,
                tentativaId,
                desafios.getFirst(),
                certas.get(desafios.getFirst()),
                status().isUnprocessableEntity());

        JsonNode resultado = concluir(sessao, tentativaId);
        assertThat(resultado.get("perfeita").asBoolean()).isFalse();
        assertThat(resultado.get("acertos").asInt()).isZero();
        assertThat(motivos(resultado)).containsExactly("PRIMEIRA_LICAO_DO_DIA");
    }

    @Test
    void naoConcluiSemResponderTudo() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        long tentativaId = iniciar(sessao, LICAO_DE_LGPD).get("tentativaId").asLong();

        pedir(post("/api/tentativas/{id}/concluir", tentativaId).cookie(sessao), status().isUnprocessableEntity());
    }

    @Test
    void proximaLicaoSoLiberaDepoisDaAnterior() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");

        assertThat(situacoes(sessao)).containsExactly("DISPONIVEL", "BLOQUEADA");
        pedir(
                post("/api/licoes/{id}/tentativas", idDaLicao(LICAO_DE_IA)).cookie(sessao),
                status().isUnprocessableEntity());

        fazerLicao(sessao, LICAO_DE_LGPD, true);

        assertThat(situacoes(sessao)).containsExactly("CONCLUIDA", "DISPONIVEL");
        JsonNode trilhaCompleta = fazerLicao(sessao, LICAO_DE_IA, true);
        assertThat(codigos(trilhaCompleta.get("trofeusNovos"))).contains("TRILHA_COMPLETA");
    }

    @Test
    void recomecarALicaoEncerraATentativaAnterior() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        Map<Long, Object> certas = gabarito(LICAO_DE_LGPD, true);
        long antiga = iniciar(sessao, LICAO_DE_LGPD).get("tentativaId").asLong();
        iniciar(sessao, LICAO_DE_LGPD);

        Map.Entry<Long, Object> primeira = certas.entrySet().iterator().next();
        responder(sessao, antiga, primeira.getKey(), primeira.getValue(), status().isUnprocessableEntity());
    }

    @Test
    void ninguemRespondeATentativaDeOutraPessoa() throws Exception {
        Map<Long, Object> certas = gabarito(LICAO_DE_LGPD, true);
        long daAna = iniciar(entrarComo("Ana Exemplo"), LICAO_DE_LGPD)
                .get("tentativaId")
                .asLong();

        Map.Entry<Long, Object> primeira = certas.entrySet().iterator().next();
        responder(
                entrarComo("Carla Demonstração"), daAna, primeira.getKey(), primeira.getValue(), status().isNotFound());
    }

    @Test
    void respostaDeOutroTipoERecusada() throws Exception {
        Cookie sessao = entrarComo("Ana Exemplo");
        long tentativaId = iniciar(sessao, LICAO_DE_LGPD).get("tentativaId").asLong();
        long primeiroDesafio = gabarito(LICAO_DE_LGPD, true).keySet().iterator().next();

        responder(sessao, tentativaId, primeiroDesafio, Map.of("valor", true), status().isUnprocessableEntity());
    }

    private List<String> situacoes(Cookie sessao) throws Exception {
        List<String> situacoes = new ArrayList<>();
        for (JsonNode modulo : ler("/api/caminho", sessao)) {
            modulo.get("licoes")
                    .forEach(licao -> situacoes.add(licao.get("situacao").asText()));
        }
        return situacoes;
    }

    private static List<String> motivos(JsonNode resultado) {
        List<String> motivos = new ArrayList<>();
        resultado.get("ganhos").forEach(ganho -> motivos.add(ganho.get("motivo").asText()));
        return motivos;
    }

    private static List<String> codigos(JsonNode trofeus) {
        List<String> codigos = new ArrayList<>();
        trofeus.forEach(trofeu -> codigos.add(trofeu.get("codigo").asText()));
        return codigos;
    }
}

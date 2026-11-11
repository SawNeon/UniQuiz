package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClassificacaoTest extends TesteDeIntegracao {

    @Test
    void mostraOTopoEQuemEstaForaDele() throws Exception {
        Cookie ana = entrarComo("Ana Exemplo");
        Cookie bruno = entrarComo("Bruno Teste");
        fazerLicao(ana, LICAO_DE_LGPD, true);

        JsonNode antesDeJogar = ler("/api/classificacao", bruno);
        assertThat(antesDeJogar.get("temporada").get("nome").asText()).isEqualTo("Temporada de exemplo");
        assertThat(antesDeJogar.get("temporada").get("premioTexto").asText()).isEqualTo("Destaque no mural da TI");
        assertThat(antesDeJogar.get("topo")).hasSize(1);
        assertThat(antesDeJogar.get("topo").get(0).get("nome").asText()).isEqualTo("Ana Exemplo");
        assertThat(antesDeJogar.get("topo").get(0).get("valor").asInt()).isEqualTo(115);
        assertThat(antesDeJogar.get("minhaPosicao")).isNull();

        fazerLicao(bruno, LICAO_DE_LGPD, false);

        JsonNode depois = ler("/api/classificacao", bruno);
        assertThat(depois.get("minhaPosicao").get("posicao").asInt()).isEqualTo(2);
        assertThat(depois.get("minhaPosicao").get("souEu").asBoolean()).isTrue();
        assertThat(depois.get("pontosParaSubir").asInt()).isEqualTo(115 - 10 + 1);
    }

    @Test
    void maisRapidosSoContaLicaoPerfeita() throws Exception {
        Cookie ana = entrarComo("Ana Exemplo");
        Cookie bruno = entrarComo("Bruno Teste");
        fazerLicao(ana, LICAO_DE_LGPD, true);
        fazerLicao(bruno, LICAO_DE_LGPD, false);

        JsonNode rapidos = ler("/api/classificacao/rapidos?licaoId=" + idDaLicao(LICAO_DE_LGPD), bruno);

        assertThat(rapidos.get("topo")).hasSize(1);
        assertThat(rapidos.get("topo").get(0).get("nome").asText()).isEqualTo("Ana Exemplo");
        assertThat(rapidos.get("minhaPosicao")).isNull();
    }

    @Test
    void zerarOsPontosTiraAPessoaDaClassificacaoSemApagarOHistorico() throws Exception {
        Cookie ana = entrarComo("Ana Exemplo");
        fazerLicao(ana, LICAO_DE_LGPD, true);

        pedir(
                post("/api/admin/colaboradores/{id}/zerar-pontos", idDoColaborador("Ana Exemplo"))
                        .cookie(entrarComoAdmin()),
                status().isNoContent());

        assertThat(ler("/api/classificacao", ana).get("topo")).isEmpty();
        assertThat(ler("/api/eu", ana).get("pontosNaTemporada").asInt()).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM evento_pontos WHERE motivo = 'AJUSTE_DO_ADMIN'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void perfilMostraPontosNivelSequenciaETrofeus() throws Exception {
        Cookie ana = entrarComo("Ana Exemplo");
        fazerLicao(ana, LICAO_DE_LGPD, true);

        JsonNode perfil = ler("/api/eu", ana);

        assertThat(perfil.get("nome").asText()).isEqualTo("Ana Exemplo");
        assertThat(perfil.get("setor").asText()).isEqualTo("Tecnologia da Informação");
        assertThat(perfil.get("pontosTotais").asInt()).isEqualTo(115);
        assertThat(perfil.get("nivel").asInt()).isEqualTo(1);
        assertThat(perfil.get("pontosParaProximoNivel").asInt()).isEqualTo(385);
        assertThat(perfil.get("diasSeguidos").asInt()).isEqualTo(1);
        assertThat(perfil.get("trofeus")).hasSize(6);
        assertThat(ler("/api/trofeus", ana).findValues("conquistadoEm")).hasSize(3);
    }

    @Test
    void temporadasNaoPodemSeSobrepor() throws Exception {
        Cookie admin = entrarComoAdmin();
        LocalDate hoje = LocalDate.now();

        pedir(
                comJson(
                        post("/api/admin/temporadas").cookie(admin),
                        Map.of(
                                "nome",
                                "Sobreposta",
                                "inicio",
                                hoje.toString(),
                                "fim",
                                hoje.plusDays(3).toString())),
                status().isUnprocessableEntity());
        pedir(
                comJson(
                        post("/api/admin/temporadas").cookie(admin),
                        Map.of(
                                "nome",
                                "Invertida",
                                "inicio",
                                hoje.plusYears(1).toString(),
                                "fim",
                                hoje.plusYears(1).minusDays(1).toString())),
                status().isUnprocessableEntity());

        JsonNode futura = pedir(
                comJson(
                        post("/api/admin/temporadas").cookie(admin),
                        Map.of(
                                "nome",
                                "Ano que vem",
                                "inicio",
                                hoje.plusYears(1).toString(),
                                "fim",
                                hoje.plusYears(1).plusDays(30).toString(),
                                "premioTexto",
                                "Brinde")),
                status().isCreated());
        pedir(
                comJson(
                        put("/api/admin/temporadas/{id}", futura.get("id").asLong())
                                .cookie(admin),
                        Map.of(
                                "nome",
                                "Ano que vem, revisada",
                                "inicio",
                                hoje.plusYears(1).toString(),
                                "fim",
                                hoje.plusYears(1).plusDays(30).toString())),
                status().isOk());

        assertThat(ler("/api/admin/temporadas", admin).findValuesAsText("nome")).contains("Ano que vem, revisada");
    }
}

package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;

class ImportacaoTest extends TesteDeIntegracao {

    private static final String PLANILHA =
            """
            nome;setor;empresa
            Ana Exemplo;Tecnologia da Informação;Unimed Exemplo
            Bruno Teste;Tecnologia da Informação;Unimed Exemplo
            Nova Pessoa;Jurídico;Unimed Exemplo
            """;

    private Cookie admin;

    @BeforeEach
    void entrar() throws Exception {
        admin = entrarComoAdmin();
    }

    @Test
    void previaNaoGravaNada() throws Exception {
        JsonNode previa = enviar("previa", "rh.csv", PLANILHA, status().isOk());

        assertThat(previa.get("novos").asInt()).isEqualTo(1);
        assertThat(previa.get("mantidos").asInt()).isEqualTo(2);
        assertThat(previa.get("inativados").asInt()).isEqualTo(1);
        assertThat(previa.get("podeConfirmar").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM colaborador", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void confirmarCriaSetorNovoEInativaQuemSaiuSemApagar() throws Exception {
        enviar("confirmar", "rh.csv", PLANILHA, status().isOk());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM colaborador", Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForObject(
                        "SELECT ativo FROM colaborador WHERE nome = N'Carla Demonstração'", Boolean.class))
                .isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM setor WHERE nome = N'Jurídico'", Integer.class))
                .isEqualTo(1);

        JsonNode segunda = enviar("previa", "rh.csv", PLANILHA, status().isOk());
        assertThat(segunda.get("novos").asInt()).isZero();
        assertThat(segunda.get("mantidos").asInt()).isEqualTo(3);
    }

    @Test
    void homonimosEImpedemAConfirmacao() throws Exception {
        String repetida = PLANILHA + "nova pessoa;JURÍDICO;Unimed Exemplo\n";

        JsonNode previa = enviar("previa", "rh.csv", repetida, status().isOk());
        assertThat(previa.get("homonimos")).hasSize(1);
        assertThat(previa.get("podeConfirmar").asBoolean()).isFalse();

        enviar("confirmar", "rh.csv", repetida, status().isUnprocessableEntity());
    }

    @Test
    void recusaArquivoQueNaoEPlanilha() throws Exception {
        enviar("previa", "rh.txt", PLANILHA, status().isUnprocessableEntity());
        enviar("previa", "rh.csv", "coluna;errada\nx;y\n", status().isUnprocessableEntity());
    }

    @Test
    void buscaEAjustaColaboradores() throws Exception {
        JsonNode encontrados = ler("/api/admin/colaboradores?busca=bruno", admin);
        assertThat(encontrados).hasSize(1);
        long bruno = encontrados.get(0).get("id").asLong();

        pedir(
                comJson(
                        patch("/api/admin/colaboradores/{id}", bruno).cookie(admin),
                        Map.of("nome", "Ana Exemplo", "ativo", true)),
                status().isUnprocessableEntity());
        pedir(
                comJson(
                        patch("/api/admin/colaboradores/{id}", bruno).cookie(admin),
                        Map.of("nome", "Bruno Teste (TI)", "ativo", true)),
                status().isNoContent());

        assertThat(ler("/api/admin/colaboradores?busca=TI)", admin)
                        .get(0)
                        .get("nome")
                        .asText())
                .isEqualTo("Bruno Teste (TI)");
        jdbc.update(
                "UPDATE colaborador SET nome = N'Bruno Teste', nome_normalizado = N'BRUNO TESTE' WHERE id = ?", bruno);
    }

    private JsonNode enviar(String etapa, String nomeDoArquivo, String conteudo, ResultMatcher esperado)
            throws Exception {
        MockMultipartFile arquivo =
                new MockMultipartFile("arquivo", nomeDoArquivo, "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
        return pedir(
                multipart("/api/admin/colaboradores/importacao/{etapa}", etapa)
                        .file(arquivo)
                        .cookie(admin),
                esperado);
    }
}

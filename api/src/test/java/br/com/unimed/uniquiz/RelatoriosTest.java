package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class RelatoriosTest extends TesteDeIntegracao {

    private static final String TI = "Tecnologia da Informação";
    private static final String FATURAMENTO = "Faturamento";

    @Test
    void conformidadeMostraQuemConcluiuQuemEstaNoMeioEQuemNaoComecou() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);

        JsonNode conformidade = ler("/api/admin/relatorios/conformidade", entrarComoAdmin());

        assertThat(conformidade.get("pessoas").asInt()).isEqualTo(3);
        assertThat(conformidade.get("totalDeLicoes").asInt()).isEqualTo(2);
        assertThat(conformidade.get("trilha").get("concluiram").asInt()).isZero();
        assertThat(conformidade.get("trilha").get("emAndamento").asInt()).isEqualTo(1);
        assertThat(conformidade.get("trilha").get("naoComecaram").asInt()).isEqualTo(2);

        JsonNode lgpd = conformidade.get("modulos").get(0);
        assertThat(lgpd.get("titulo").asText()).isEqualTo("LGPD na prática");
        assertThat(lgpd.get("concluiram").asInt()).isEqualTo(1);
        assertThat(lgpd.get("naoComecaram").asInt()).isEqualTo(2);
        assertThat(conformidade.get("modulos").get(1).get("naoComecaram").asInt())
                .isEqualTo(3);

        JsonNode ti = setor(conformidade, TI);
        assertThat(ti.get("pessoas").asInt()).isEqualTo(2);
        assertThat(ti.get("licoesConcluidas").asDouble()).isEqualTo(25.0);
        assertThat(setor(conformidade, FATURAMENTO).get("licoesConcluidas").asDouble())
                .isZero();
    }

    @Test
    void listaDePessoasFiltraPorModuloSituacaoEBusca() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);
        Cookie admin = entrarComoAdmin();
        long lgpd = idDoModuloDaLicao(LICAO_DE_LGPD);

        JsonNode concluiram =
                ler("/api/admin/relatorios/conformidade/pessoas?moduloId=" + lgpd + "&situacao=CONCLUIU", admin);
        assertThat(nomes(concluiram.get("itens"))).containsExactly("Ana Exemplo");
        assertThat(concluiram.get("itens").get(0).get("ultimaAtividade").isNull())
                .isFalse();

        JsonNode naoComecaram =
                ler("/api/admin/relatorios/conformidade/pessoas?moduloId=" + lgpd + "&situacao=NAO_COMECOU", admin);
        assertThat(nomes(naoComecaram.get("itens"))).containsExactly("Bruno Teste", "Carla Demonstração");
        assertThat(naoComecaram.get("total").asInt()).isEqualTo(2);

        JsonNode busca = ler("/api/admin/relatorios/conformidade/pessoas?busca=carla", admin);
        assertThat(nomes(busca.get("itens"))).containsExactly("Carla Demonstração");
    }

    @Test
    void gestorSoVeOsPropriosSetores() throws Exception {
        fazerLicao(entrarComo("Carla Demonstração"), LICAO_DE_LGPD, false);
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);
        Cookie gestor = entrarNoPainelComo("gestor.ti", "GESTOR", Set.of(idDoSetor(TI)));

        JsonNode conformidade = ler("/api/admin/relatorios/conformidade", gestor);
        assertThat(conformidade.get("pessoas").asInt()).isEqualTo(2);
        assertThat(conformidade.get("setores").findValuesAsText("setor")).containsExactly(TI);

        assertThat(nomes(ler("/api/admin/relatorios/conformidade/pessoas", gestor)
                        .get("itens")))
                .containsExactly("Ana Exemplo", "Bruno Teste");
        assertThat(ler("/api/admin/relatorios/filtros", gestor).get("setores").findValuesAsText("nome"))
                .containsExactly(TI);
        mvc.perform(get("/api/admin/relatorios/conformidade?setorId=" + idDoSetor(FATURAMENTO))
                        .cookie(gestor))
                .andExpect(status().isNotFound());

        JsonNode desempenho = ler("/api/admin/relatorios/desempenho", gestor);
        assertThat(desempenho.get("licoes").get(0).get("iniciadas").asInt()).isEqualTo(1);
        assertThat(desempenho.get("licoes").get(0).get("acertoNaPrimeira").asDouble())
                .isEqualTo(100.0);
    }

    @Test
    void desempenhoMostraAcertoAbandonoEOErroMaisEscolhido() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);
        fazerLicao(entrarComo("Bruno Teste"), LICAO_DE_LGPD, false);
        Cookie carla = entrarComo("Carla Demonstração");
        iniciar(carla, LICAO_DE_LGPD);
        iniciar(carla, LICAO_DE_LGPD);

        JsonNode desempenho = ler("/api/admin/relatorios/desempenho", entrarComoAdmin());

        JsonNode licao = desempenho.get("licoes").get(0);
        assertThat(licao.get("licao").asText()).isEqualTo(LICAO_DE_LGPD);
        assertThat(licao.get("iniciadas").asInt()).isEqualTo(4);
        assertThat(licao.get("concluidas").asInt()).isEqualTo(2);
        assertThat(licao.get("perfeitas").asInt()).isEqualTo(1);
        assertThat(licao.get("abandonadas").asInt()).isEqualTo(1);
        assertThat(licao.get("acertoNaPrimeira").asDouble()).isEqualTo(50.0);

        JsonNode desafios = desempenho.get("desafios");
        assertThat(desafios).hasSize(5);
        for (JsonNode desafio : desafios) {
            assertThat(desafio.get("respostas").asInt()).isEqualTo(2);
            assertThat(desafio.get("acerto").asDouble()).isEqualTo(50.0);
            assertThat(desafio.get("calibracao").asText()).isEqualTo("POUCOS_DADOS");
            assertThat(desafio.get("erroMaisComum").asText()).isNotBlank();
        }
        assertThat(desafios.findValuesAsText("erroMaisComum")).contains("Apenas os dados das empresas", "Falso");
        assertThat(desafios.findValuesAsText("enunciado"))
                .doesNotContain("Verdadeiro ou falso?", "Complete a frase.")
                .allSatisfy(texto -> assertThat(texto).isNotBlank().doesNotContain("{{"));
    }

    @Test
    void desempenhoRecusaPeriodoInvertido() throws Exception {
        mvc.perform(get("/api/admin/relatorios/desempenho?de=2026-11-10&ate=2026-11-01")
                        .cookie(entrarComoAdmin()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void engajamentoContaPessoasAtivasPorSemana() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);
        fazerLicao(entrarComo("Bruno Teste"), LICAO_DE_LGPD, true);

        JsonNode engajamento = ler("/api/admin/relatorios/engajamento?semanas=4", entrarComoAdmin());

        assertThat(engajamento.get("pessoasNoEscopo").asInt()).isEqualTo(3);
        assertThat(engajamento.get("semanas")).hasSize(4);
        JsonNode atual = engajamento.get("semanas").get(3);
        assertThat(atual.get("pessoasAtivas").asInt()).isEqualTo(2);
        assertThat(atual.get("licoesConcluidas").asInt()).isEqualTo(2);
        assertThat(atual.get("novosParticipantes").asInt()).isEqualTo(2);
        assertThat(atual.get("participacao").asDouble()).isEqualTo(66.7);
        assertThat(engajamento.get("semanas").get(0).get("pessoasAtivas").asInt())
                .isZero();

        mvc.perform(get("/api/admin/relatorios/engajamento?semanas=0").cookie(entrarComoAdmin()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exportaCsvProtegidoContraFormulaERegistraNaAuditoria() throws Exception {
        Cookie admin = entrarComoAdmin();
        pedir(
                comJson(
                        post("/api/admin/colaboradores").cookie(admin),
                        Map.of("nome", "=HYPERLINK(\"http://golpe\")", "empresa", "Unimed Exemplo", "setor", TI)),
                status().isCreated());
        long ultimoRegistroAntes = ultimoRegistroDeAuditoria();

        byte[] arquivo = mvc.perform(get("/api/admin/relatorios/conformidade-pessoas/exportar?formato=CSV")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("conformidade-por-pessoa")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        String csv = new String(arquivo, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿Nome;Empresa;Setor;Situação");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://golpe\"\")\"").contains("Carla Demonstração;Unimed Exemplo");
        assertThat(jdbc.queryForObject(
                        "SELECT alvo FROM registro_de_auditoria WHERE id > ? AND acao = 'RELATORIO_EXPORTADO'",
                        String.class,
                        ultimoRegistroAntes))
                .isEqualTo("relatorio=conformidade-pessoas, formato=csv");
    }

    @Test
    void exportaExcelComTextoQueNuncaViraFormula() throws Exception {
        Cookie admin = entrarComoAdmin();
        pedir(
                comJson(
                        post("/api/admin/colaboradores").cookie(admin),
                        Map.of("nome", "=1+1", "empresa", "Unimed Exemplo", "setor", TI)),
                status().isCreated());

        byte[] arquivo = mvc.perform(get("/api/admin/relatorios/conformidade-pessoas/exportar?formato=XLSX")
                        .cookie(admin))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (XSSFWorkbook pasta = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            Sheet aba = pasta.getSheetAt(0);
            assertThat(aba.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nome");
            Row formula = aba.getRow(1);
            assertThat(formula.getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(formula.getCell(0).getStringCellValue()).isEqualTo("=1+1");
            assertThat(aba.getLastRowNum()).isEqualTo(4);
        }
    }

    @Test
    void exportaTodosOsRelatoriosParaOAuditor() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, false);
        Cookie auditor = entrarNoPainelComo("auditor.teste", "AUDITOR", Set.of());

        for (String relatorio :
                List.of("conformidade-pessoas", "desempenho-licoes", "desempenho-desafios", "engajamento")) {
            String csv = mvc.perform(get("/api/admin/relatorios/" + relatorio + "/exportar")
                            .cookie(auditor))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(csv.lines().count()).as(relatorio).isGreaterThan(1);
        }
        mvc.perform(get("/api/admin/relatorios/inexistente/exportar").cookie(auditor))
                .andExpect(status().isNotFound());
    }

    @Test
    void relatoriosExigemPainelESenhaDefinitiva() throws Exception {
        mvc.perform(get("/api/admin/relatorios/conformidade")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/relatorios/conformidade").cookie(entrarComo("Ana Exemplo")))
                .andExpect(status().isUnauthorized());

        String provisoria = pedir(
                        comJson(
                                post("/api/admin/usuarios").cookie(entrarComoAdmin()),
                                Map.of(
                                        "nome",
                                        "Nova",
                                        "usuario",
                                        "nova.gestora",
                                        "perfil",
                                        "AUDITOR",
                                        "setores",
                                        Set.of())),
                        status().isCreated())
                .get("senha")
                .asText();
        Cookie semTrocar = mvc.perform(
                        comJson(post("/api/admin/login"), Map.of("usuario", "nova.gestora", "senha", provisoria)))
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");
        mvc.perform(get("/api/admin/relatorios/conformidade").cookie(semTrocar)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/relatorios/engajamento/exportar").cookie(semTrocar))
                .andExpect(status().isForbidden());
    }

    private long ultimoRegistroDeAuditoria() {
        return jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM registro_de_auditoria", Long.class);
    }

    private static JsonNode setor(JsonNode conformidade, String nome) {
        for (JsonNode setor : conformidade.get("setores")) {
            if (setor.get("setor").asText().equals(nome)) {
                return setor;
            }
        }
        throw new AssertionError("Setor não encontrado: " + nome);
    }

    private static List<String> nomes(JsonNode itens) {
        List<String> nomes = new ArrayList<>();
        itens.forEach(item -> nomes.add(item.get("nome").asText()));
        return nomes;
    }
}

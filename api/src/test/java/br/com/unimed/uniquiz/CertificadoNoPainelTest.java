package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class CertificadoNoPainelTest extends TesteDeIntegracao {

    @Test
    void moduloTravadoGuardaOCertificadoMasSoDeixaBaixarDepoisDeLiberar() throws Exception {
        long moduloId = idDoModuloDaLicao(LICAO_DE_LGPD);
        Cookie admin = entrarComoAdmin();
        liberar(admin, moduloId, false);
        Cookie ana = entrarComo("Ana Exemplo");

        JsonNode resultado = fazerLicao(ana, LICAO_DE_LGPD, true);

        JsonNode concluido = resultado.get("moduloConcluido");
        assertThat(concluido.get("certificadoLiberado").asBoolean()).isFalse();
        String codigo = concluido.get("codigoDoCertificado").asText();
        assertThat(ler("/api/certificados", ana).get(0).get("liberado").asBoolean())
                .isFalse();
        assertThat(moduloNoCaminho(ana, moduloId).get("certificadoLiberado").asBoolean())
                .isFalse();
        mvc.perform(get("/api/certificados/{codigo}/pdf", codigo).cookie(ana))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/publico/certificados/{codigo}", codigo)).andExpect(status().isNotFound());
        assertThat(pedir(post("/api/modulos/{id}/certificado", moduloId).cookie(ana), status().isOk())
                        .get("codigo")
                        .asText())
                .isEqualTo(codigo);

        liberar(admin, moduloId, true);

        mvc.perform(get("/api/certificados/{codigo}/pdf", codigo).cookie(ana))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(get("/api/publico/certificados/{codigo}", codigo)).andExpect(status().isOk());
        assertThat(ultimaAuditoria("LIBERACAO_DO_CERTIFICADO_ALTERADA"))
                .containsEntry("alvo", "moduloId=%d, liberado=true".formatted(moduloId));
    }

    @Test
    void moduloNovoComecaComOCertificadoTravado() throws Exception {
        Cookie admin = entrarComoAdmin();
        long novo = pedir(
                        comJson(
                                post("/api/admin/modulos").cookie(admin),
                                Map.of("titulo", "Golpes digitais", "descricao", "")),
                        status().isCreated())
                .get("id")
                .asLong();

        JsonNode modulos = ler("/api/admin/certificados/modulos", admin);

        assertThat(modulos.findValues("id").stream().map(JsonNode::asLong)).contains(novo);
        for (JsonNode modulo : modulos) {
            boolean esperado = modulo.get("id").asLong() != novo;
            assertThat(modulo.get("liberado").asBoolean()).isEqualTo(esperado);
        }
    }

    @Test
    void listaEExportaOsCertificadosEmZipComUmPdfPorPessoa() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);
        fazerLicao(entrarComo("Carla Demonstração"), LICAO_DE_LGPD, true);
        Cookie admin = entrarComoAdmin();

        JsonNode pagina = ler("/api/admin/certificados", admin);
        assertThat(pagina.get("total").asLong()).isEqualTo(2);
        assertThat(pagina.get("itens").get(0).get("codigoFormatado").asText()).hasSize(14);
        assertThat(ler("/api/admin/certificados?busca=carla", admin)
                        .get("total")
                        .asLong())
                .isEqualTo(1);

        Map<String, byte[]> arquivos =
                zip(mvc.perform(get("/api/admin/certificados/exportar").cookie(admin))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("application/zip"))
                        .andExpect(header().string("Content-Disposition", Matchers.containsString(".zip")))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray());

        assertThat(arquivos.keySet())
                .hasSize(3)
                .contains("lista-de-certificados.csv")
                .anyMatch(nome -> nome.startsWith("LGPD na prática/Ana Exemplo - ") && nome.endsWith(".pdf"))
                .anyMatch(nome -> nome.startsWith("LGPD na prática/Carla Demonstração - "));
        arquivos.forEach((nome, conteudo) -> {
            if (nome.endsWith(".pdf")) {
                assertThat(new String(conteudo, 0, 5, StandardCharsets.US_ASCII))
                        .isEqualTo("%PDF-");
            }
        });
        assertThat(new String(arquivos.get("lista-de-certificados.csv"), StandardCharsets.UTF_8))
                .contains("Ana Exemplo")
                .contains("Faturamento");
        assertThat(ultimaAuditoria("CERTIFICADOS_EXPORTADOS"))
                .containsEntry("alvo", "todos")
                .containsEntry("detalhe", "2 certificados");
    }

    @Test
    void exportarSemNenhumCertificadoAvisaEmVezDeBaixarZipVazio() throws Exception {
        mvc.perform(get("/api/admin/certificados/exportar").cookie(entrarComoAdmin()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void gestorSoVeEExportaOsCertificadosDosPropriosSetoresENaoMexeNoModelo() throws Exception {
        fazerLicao(entrarComo("Ana Exemplo"), LICAO_DE_LGPD, true);
        fazerLicao(entrarComo("Carla Demonstração"), LICAO_DE_LGPD, true);
        long ti = idDoSetor("Tecnologia da Informação");
        Cookie gestor = entrarNoPainelComo("gestor.ti", "GESTOR", Set.of(ti));

        JsonNode pagina = ler("/api/admin/certificados", gestor);
        assertThat(pagina.get("total").asLong()).isEqualTo(1);
        assertThat(pagina.get("itens").get(0).get("nome").asText()).isEqualTo("Ana Exemplo");
        assertThat(zip(mvc.perform(get("/api/admin/certificados/exportar").cookie(gestor))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsByteArray())
                        .keySet())
                .hasSize(2);
        mvc.perform(get("/api/admin/certificados")
                        .param("setorId", String.valueOf(idDoSetor("Faturamento")))
                        .cookie(gestor))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/certificados/modelo").cookie(gestor)).andExpect(status().isForbidden());
        mvc.perform(comJson(
                        post("/api/admin/certificados/modulos/{id}/liberacao", idDoModuloDaLicao(LICAO_DE_LGPD))
                                .cookie(gestor),
                        Map.of("liberado", false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void modeloGuardaAsPosicoesEAArteDeFundo() throws Exception {
        Cookie admin = entrarComoAdmin();
        JsonNode padrao = ler("/api/admin/certificados/modelo", admin);
        assertThat(padrao.get("temArte").asBoolean()).isFalse();
        assertThat(padrao.get("campos")).hasSize(5);

        List<Map<String, Object>> campos = campos(padrao);
        campos.get(0).put("x", 30);
        campos.get(0).put("alinhamento", "ESQUERDA");
        JsonNode salvo = pedir(
                comJson(put("/api/admin/certificados/modelo").cookie(admin), Map.of("campos", campos)),
                status().isOk());
        assertThat(salvo.get("campos").get(0).get("x").asDouble()).isEqualTo(30);
        assertThat(salvo.get("campos").get(0).get("alinhamento").asText()).isEqualTo("ESQUERDA");

        campos.get(1).put("cor", "verde");
        mvc.perform(comJson(put("/api/admin/certificados/modelo").cookie(admin), Map.of("campos", campos)))
                .andExpect(status().isUnprocessableEntity());

        JsonNode comArte = pedir(
                multipart("/api/admin/certificados/modelo/arte")
                        .file(new MockMultipartFile("arquivo", "arte.png", "image/png", png(1200, 1000)))
                        .cookie(admin),
                status().isOk());
        assertThat(comArte.get("temArte").asBoolean()).isTrue();
        assertThat(comArte.get("nomeDaArte").asText()).isEqualTo("arte.png");
        assertThat(comArte.get("avisos")).isNotEmpty();
        assertThat(comArte.get("campos").get(0).get("x").asDouble()).isEqualTo(30);

        mvc.perform(get("/api/admin/certificados/modelo/arte").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        byte[] previa = mvc.perform(comJson(
                        post("/api/admin/certificados/modelo/previa").cookie(admin), Map.of("campos", campos(salvo))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        try (PDDocument documento = Loader.loadPDF(previa)) {
            assertThat(documento.getPage(0).getResources().getXObjectNames()).isNotEmpty();
        }

        mvc.perform(multipart("/api/admin/certificados/modelo/arte")
                        .file(new MockMultipartFile("arquivo", "planilha.csv", "text/csv", "a,b".getBytes()))
                        .cookie(admin))
                .andExpect(status().isUnprocessableEntity());

        JsonNode semArte = pedir(delete("/api/admin/certificados/modelo/arte").cookie(admin), status().isOk());
        assertThat(semArte.get("temArte").asBoolean()).isFalse();
        mvc.perform(get("/api/admin/certificados/modelo/arte").cookie(admin)).andExpect(status().isNotFound());

        assertThat(ultimaAuditoria("MODELO_DE_CERTIFICADO_EDITADO")).containsEntry("resultado", "SUCESSO");
        assertThat(ultimaAuditoria("ARTE_DO_CERTIFICADO_ENVIADA")).containsEntry("alvo", "arte=planilha.csv");
        assertThat(ultimaAuditoria("ARTE_DO_CERTIFICADO_REMOVIDA")).containsEntry("resultado", "SUCESSO");
    }

    @Test
    void certificadoBaixadoUsaAArteEnviada() throws Exception {
        Cookie admin = entrarComoAdmin();
        mvc.perform(multipart("/api/admin/certificados/modelo/arte")
                        .file(new MockMultipartFile("arquivo", "arte.png", "image/png", png(3508, 2480)))
                        .cookie(admin))
                .andExpect(status().isOk());
        Cookie ana = entrarComo("Ana Exemplo");
        String codigo = fazerLicao(ana, LICAO_DE_LGPD, true)
                .get("moduloConcluido")
                .get("codigoDoCertificado")
                .asText();

        byte[] pdf = mvc.perform(get("/api/certificados/{codigo}/pdf", codigo).cookie(ana))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getPage(0).getResources().getXObjectNames()).isNotEmpty();
        }
    }

    private void liberar(Cookie admin, long moduloId, boolean liberado) throws Exception {
        mvc.perform(comJson(
                        post("/api/admin/certificados/modulos/{id}/liberacao", moduloId)
                                .cookie(admin),
                        Map.of("liberado", liberado)))
                .andExpect(status().isNoContent());
    }

    private JsonNode moduloNoCaminho(Cookie sessao, long moduloId) throws Exception {
        for (JsonNode modulo : ler("/api/caminho", sessao)) {
            if (modulo.get("id").asLong() == moduloId) {
                return modulo;
            }
        }
        throw new IllegalStateException("Módulo fora do caminho");
    }

    private Map<String, Object> ultimaAuditoria(String acao) {
        return jdbc.queryForMap(
                "SELECT TOP 1 alvo, detalhe, resultado FROM registro_de_auditoria WHERE acao = ? ORDER BY id DESC",
                acao);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> campos(JsonNode modelo) {
        List<Map<String, Object>> campos = new ArrayList<>();
        modelo.get("campos").forEach(campo -> campos.add(json.convertValue(campo, Map.class)));
        return campos;
    }

    private static Map<String, byte[]> zip(byte[] conteudo) throws IOException {
        Map<String, byte[]> arquivos = new LinkedHashMap<>();
        try (ZipInputStream entrada = new ZipInputStream(new ByteArrayInputStream(conteudo))) {
            ZipEntry item;
            while ((item = entrada.getNextEntry()) != null) {
                arquivos.put(item.getName(), entrada.readAllBytes());
            }
        }
        return arquivos;
    }

    private static byte[] png(int largura, int altura) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D desenho = imagem.createGraphics();
        desenho.setColor(new Color(0x00, 0x99, 0x5d));
        desenho.fillRect(0, 0, largura, altura);
        desenho.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        }
    }
}

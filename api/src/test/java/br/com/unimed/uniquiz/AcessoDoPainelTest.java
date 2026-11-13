package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.unimed.uniquiz.auditoria.dominio.CatalogoDeRotas;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class AcessoDoPainelTest extends TesteDeIntegracao {

    private static final String NOVA_SENHA = "uma frase longa de teste";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;

    private record Conta(long id, String usuario, Cookie sessao) {}

    private JsonNode criar(String usuario, String perfil, Set<Long> setores) throws Exception {
        return pedir(
                comJson(
                        post("/api/admin/usuarios").cookie(entrarComoAdmin()),
                        Map.of("nome", "Pessoa " + usuario, "usuario", usuario, "perfil", perfil, "setores", setores)),
                status().isCreated());
    }

    private Cookie entrar(String usuario, String senha) throws Exception {
        return mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", usuario, "senha", senha)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("uniquiz_admin");
    }

    private Conta contaPronta(String usuario, String perfil, Set<Long> setores) throws Exception {
        JsonNode criada = criar(usuario, perfil, setores);
        Cookie sessao = entrar(usuario, criada.get("senha").asText());
        mvc.perform(comJson(
                        post("/api/admin/minha-conta/senha").cookie(sessao),
                        Map.of("senhaAtual", criada.get("senha").asText(), "novaSenha", NOVA_SENHA)))
                .andExpect(status().isNoContent());
        return new Conta(criada.get("usuarioId").asLong(), usuario, sessao);
    }

    private long umSetor() {
        return jdbc.queryForObject("SELECT MIN(id) FROM setor", Long.class);
    }

    private static ResultMatcher permitido() {
        return resultado -> assertThat(resultado.getResponse().getStatus())
                .as("status de %s", resultado.getRequest().getRequestURI())
                .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest(name = "{0} em {1}: {2}")
    @CsvSource({
        "ADMINISTRADOR, /api/admin/modulos, sim",
        "ADMINISTRADOR, /api/admin/colaboradores?trecho=ana, sim",
        "ADMINISTRADOR, /api/admin/temporadas, sim",
        "ADMINISTRADOR, /api/admin/usuarios, sim",
        "GESTOR, /api/admin/modulos, nao",
        "GESTOR, /api/admin/colaboradores?trecho=ana, nao",
        "GESTOR, /api/admin/temporadas, nao",
        "GESTOR, /api/admin/usuarios, nao",
        "GESTOR, /api/admin/eu, sim",
        "AUDITOR, /api/admin/modulos, nao",
        "AUDITOR, /api/admin/colaboradores?trecho=ana, nao",
        "AUDITOR, /api/admin/temporadas, nao",
        "AUDITOR, /api/admin/usuarios, nao",
        "AUDITOR, /api/admin/eu, sim"
    })
    void cadaPerfilAlcancaSoOQueLheCabe(String perfil, String rota, String alcanca) throws Exception {
        Set<Long> setores = "GESTOR".equals(perfil) ? Set.of(umSetor()) : Set.of();
        Conta conta = contaPronta(perfil.toLowerCase() + ".teste", perfil, setores);

        mvc.perform(get(rota).cookie(conta.sessao()))
                .andExpect("sim".equals(alcanca) ? permitido() : status().isForbidden());
    }

    @Test
    void acessoNegadoVoltaComoProblemDetail() throws Exception {
        Conta auditor = contaPronta("auditor.teste", "AUDITOR", Set.of());

        mvc.perform(get("/api/admin/modulos").cookie(auditor.sessao()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sem permissão"));
    }

    @Test
    void meusDadosTrazemPerfilEPermissoes() throws Exception {
        Conta auditor = contaPronta("auditor.teste", "AUDITOR", Set.of());

        mvc.perform(get("/api/admin/eu").cookie(auditor.sessao()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("auditor.teste"))
                .andExpect(jsonPath("$.perfil").value("AUDITOR"))
                .andExpect(jsonPath("$.precisaTrocarSenha").value(false))
                .andExpect(jsonPath("$.permissoes.length()").value(3));
    }

    @Test
    void senhaProvisoriaSoDeixaTrocarASenha() throws Exception {
        JsonNode criada = criar("novo.admin", "ADMINISTRADOR", Set.of());
        Cookie sessao = entrar("novo.admin", criada.get("senha").asText());

        mvc.perform(get("/api/admin/eu").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precisaTrocarSenha").value(true))
                .andExpect(jsonPath("$.permissoes.length()").value(0));
        mvc.perform(get("/api/admin/modulos").cookie(sessao)).andExpect(status().isForbidden());

        mvc.perform(comJson(
                        post("/api/admin/minha-conta/senha").cookie(sessao),
                        Map.of("senhaAtual", criada.get("senha").asText(), "novaSenha", NOVA_SENHA)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/modulos").cookie(sessao)).andExpect(status().isOk());
    }

    @Test
    void trocarASenhaDerrubaAsOutrasSessoes() throws Exception {
        Conta conta = contaPronta("gestor.teste", "GESTOR", Set.of(umSetor()));
        Cookie outroAparelho = entrar(conta.usuario(), NOVA_SENHA);

        mvc.perform(comJson(
                        post("/api/admin/minha-conta/senha").cookie(conta.sessao()),
                        Map.of("senhaAtual", NOVA_SENHA, "novaSenha", "outra frase longa de teste")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/eu").cookie(outroAparelho)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/eu").cookie(conta.sessao())).andExpect(status().isOk());
    }

    @Test
    void contaDesativadaPerdeASessaoNaHoraENaoEntraMais() throws Exception {
        Conta conta = contaPronta("auditor.teste", "AUDITOR", Set.of());

        mvc.perform(post("/api/admin/usuarios/{id}/desativar", conta.id()).cookie(entrarComoAdmin()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/eu").cookie(conta.sessao())).andExpect(status().isUnauthorized());
        mvc.perform(comJson(post("/api/admin/login"), Map.of("usuario", conta.usuario(), "senha", NOVA_SENHA)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void mudarOPerfilValeNaProximaRequisicao() throws Exception {
        Conta conta = contaPronta("auditor.teste", "AUDITOR", Set.of());
        mvc.perform(get("/api/admin/modulos").cookie(conta.sessao())).andExpect(status().isForbidden());

        mvc.perform(comJson(
                        put("/api/admin/usuarios/{id}", conta.id()).cookie(entrarComoAdmin()),
                        Map.of("nome", "Promovida", "perfil", "ADMINISTRADOR", "setores", Set.of())))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/modulos").cookie(conta.sessao())).andExpect(status().isOk());
    }

    @Test
    void ninguemDesativaAPropriaConta() throws Exception {
        long admin = jdbc.queryForObject("SELECT id FROM administrador WHERE usuario = 'admin'", Long.class);

        mvc.perform(post("/api/admin/usuarios/{id}/desativar", admin).cookie(entrarComoAdmin()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Você não pode desativar a própria conta."));
    }

    @Test
    void acoesDoPainelFicamNoRegistroDeAuditoria() throws Exception {
        long antes = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM registro_de_auditoria", Long.class);

        JsonNode criada = criar("auditor.teste", "AUDITOR", Set.of());

        Map<String, Object> registro = jdbc.queryForMap(
                "SELECT acao, alvo, resultado, ator_tipo FROM registro_de_auditoria WHERE id > ? AND acao = ?",
                antes,
                "USUARIO_DO_PAINEL_CRIADO");
        assertThat(registro).containsEntry("resultado", "SUCESSO").containsEntry("ator_tipo", "ADMINISTRADOR");
        assertThat(criada.get("senha").asText()).isNotBlank();
    }

    @Test
    void registroDeAuditoriaNaoAceitaAlteracaoNemExclusao() {
        jdbc.update(
                "INSERT INTO registro_de_auditoria (momento, ator_tipo, acao, resultado) VALUES (SYSUTCDATETIME(), 'ANONIMO', 'TESTE', 'SUCESSO')");

        assertThatThrownBy(() -> jdbc.update("UPDATE registro_de_auditoria SET acao = 'OUTRA'"))
                .hasMessageContaining("apenas inclusões");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM registro_de_auditoria"))
                .hasMessageContaining("apenas inclusões");
    }

    @Test
    void todaRotaDoPainelQueAlteraDadosEstaNoCatalogoDeAuditoria() {
        List<String> semAcao = new ArrayList<>();
        mapeamentos.getHandlerMethods().keySet().forEach(mapeamento -> {
            for (RequestMethod metodo : mapeamento.getMethodsCondition().getMethods()) {
                if (metodo == RequestMethod.GET) {
                    continue;
                }
                for (String rota : mapeamento.getPatternValues()) {
                    if (rota.startsWith("/api/admin") && !CatalogoDeRotas.conhecida(metodo.name(), rota)) {
                        semAcao.add(metodo + " " + rota);
                    }
                }
            }
        });

        assertThat(semAcao).isEmpty();
    }
}

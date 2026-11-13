package br.com.unimed.uniquiz;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EntradaDoColaboradorTest extends TesteDeIntegracao {

    private long setorDaAna() {
        return jdbc.queryForObject("SELECT setor_id FROM colaborador WHERE nome = ?", Long.class, "Ana Exemplo");
    }

    @Test
    void listaDeNomesNaoEhPublicaSemBusca() throws Exception {
        mvc.perform(get("/api/setores/{id}/colaboradores", setorDaAna())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/setores/{id}/colaboradores", setorDaAna()).param("busca", "an"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Digite pelo menos 3 letras do seu nome."));
    }

    @Test
    void buscaIgnoraAcentoEDevolveNoMaximoDez() throws Exception {
        long setor = setorDaAna();
        long empresa = jdbc.queryForObject("SELECT empresa_id FROM setor WHERE id = ?", Long.class, setor);
        for (int numero = 1; numero <= 12; numero++) {
            jdbc.update(
                    "INSERT INTO colaborador (empresa_id, setor_id, nome, nome_normalizado, ativo, criado_em)"
                            + " VALUES (?, ?, ?, ?, 1, SYSUTCDATETIME())",
                    empresa,
                    setor,
                    "Márcia Teste " + numero,
                    "MARCIA TESTE " + numero);
        }

        mvc.perform(get("/api/setores/{id}/colaboradores", setor).param("busca", "marc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].nome").value("Márcia Teste 1"));
    }

    @Test
    void inativosNaoAparecemNaBusca() throws Exception {
        jdbc.update("UPDATE colaborador SET ativo = 0 WHERE nome = ?", "Ana Exemplo");

        mvc.perform(get("/api/setores/{id}/colaboradores", setorDaAna()).param("busca", "ana"))
                .andExpect(jsonPath("$[?(@.nome == 'Ana Exemplo')]").isEmpty());
    }

    @Test
    void aparelhoCompartilhadoGanhaSessaoCurta() throws Exception {
        mvc.perform(comJson(
                        post("/api/sessao"),
                        Map.of(
                                "colaboradorId",
                                idDoColaborador("Ana Exemplo"),
                                "pin",
                                PIN_DE_TESTE,
                                "aparelhoCompartilhado",
                                true)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=7200")));

        mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", idDoColaborador("Bruno Teste"), "pin", "321")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=2592000")));
    }

    @Test
    void pinErradoAvisaQuantasTentativasRestam() throws Exception {
        long ana = idDoColaborador("Ana Exemplo");
        entrarComo("Ana Exemplo");
        for (int erro = 0; erro < 2; erro++) {
            mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", ana, "pin", "999")));
        }

        mvc.perform(comJson(post("/api/sessao"), Map.of("colaboradorId", ana, "pin", "999")))
                .andExpect(jsonPath("$.detail").value("PIN incorreto. Restam 2 tentativas antes do bloqueio."));
    }
}

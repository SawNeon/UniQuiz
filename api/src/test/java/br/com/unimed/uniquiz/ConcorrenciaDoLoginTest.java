package br.com.unimed.uniquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ConcorrenciaDoLoginTest extends TesteDeIntegracao {

    @Test
    void chutesDeSenhaEmParaleloNaoPassamDoLimiteNemEsgotamAsConexoes() throws Exception {
        long ultimoRegistroAntes =
                jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM registro_de_auditoria", Long.class);
        ExecutorService pool = Executors.newFixedThreadPool(30);
        try {
            List<Future<String>> respostas = IntStream.range(0, 60)
                    .mapToObj(chute -> pool.submit(() -> mvc.perform(
                                    comJson(post("/api/admin/login"), Map.of("usuario", "admin", "senha", "errada")))
                            .andReturn()
                            .getResponse()
                            .getContentAsString()))
                    .toList();
            long senhasAvaliadas = 0;
            for (Future<String> resposta : respostas) {
                if (resposta.get(20, TimeUnit.SECONDS).contains("Usuário ou senha inválidos.")) {
                    senhasAvaliadas++;
                }
            }
            assertThat(senhasAvaliadas).isEqualTo(5);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registro_de_auditoria"
                                + " WHERE id > ? AND acao = 'ENTRADA_NO_PAINEL' AND resultado = 'FALHA'",
                        Integer.class,
                        ultimoRegistroAntes))
                .isEqualTo(60);
    }
}

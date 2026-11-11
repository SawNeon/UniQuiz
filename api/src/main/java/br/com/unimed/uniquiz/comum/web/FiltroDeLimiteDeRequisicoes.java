package br.com.unimed.uniquiz.comum.web;

import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class FiltroDeLimiteDeRequisicoes extends OncePerRequestFilter {

    private final Map<String, Bucket> baldes = new ConcurrentHashMap<>();
    private final PropriedadesDoUniQuiz.LimiteRequisicoes limites;
    private final ObjectMapper json;

    public FiltroDeLimiteDeRequisicoes(PropriedadesDoUniQuiz.LimiteRequisicoes limites, ObjectMapper json) {
        this.limites = limites;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        boolean loginAdmin =
                "POST".equals(requisicao.getMethod()) && "/api/admin/login".equals(requisicao.getRequestURI());
        int porMinuto = loginAdmin ? limites.loginAdminPorMinuto() : limites.geralPorMinuto();
        String chave = (loginAdmin ? "login:" : "geral:") + requisicao.getRemoteAddr();

        Bucket balde = baldes.computeIfAbsent(chave, ignorado -> novoBalde(porMinuto));
        if (balde.tryConsume(1)) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }
        Problemas.escrever(
                resposta,
                json,
                HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas",
                "Você fez muitas tentativas seguidas. Aguarde um minuto e tente de novo.");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !requisicao.getRequestURI().startsWith("/api/");
    }

    public void esvaziar() {
        baldes.clear();
    }

    private static Bucket novoBalde(int porMinuto) {
        return Bucket.builder()
                .addLimit(limite -> limite.capacity(porMinuto).refillGreedy(porMinuto, Duration.ofMinutes(1)))
                .build();
    }
}

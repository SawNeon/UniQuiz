package br.com.unimed.uniquiz.comum.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

public final class Problemas {

    private Problemas() {}

    public static ProblemDetail criar(HttpStatus status, String titulo, String mensagem) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setTitle(titulo);
        return problema;
    }

    public static void escrever(
            HttpServletResponse resposta, ObjectMapper json, HttpStatus status, String titulo, String mensagem)
            throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), criar(status, titulo, mensagem));
    }
}

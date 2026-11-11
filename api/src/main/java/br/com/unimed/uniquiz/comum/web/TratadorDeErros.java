package br.com.unimed.uniquiz.comum.web;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(ErroDeNegocio.class)
    ProblemDetail erroDeNegocio(ErroDeNegocio erro) {
        return Problemas.criar(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível concluir", erro.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontrado.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontrado erro) {
        return Problemas.criar(HttpStatus.NOT_FOUND, "Não encontrado", erro.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception erro) {
        log.error("Erro inesperado ao atender a requisição", erro);
        return Problemas.criar(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado", "Algo deu errado. Tente de novo em instantes.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException erro, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult()
                .getFieldErrors()
                .forEach(campo -> campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        ProblemDetail problema =
                Problemas.criar(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os campos informados.");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }
}

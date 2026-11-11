package br.com.unimed.uniquiz.conteudo.aplicacao;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoDesafio;
import br.com.unimed.uniquiz.conteudo.dominio.RespostaDoColaborador;
import br.com.unimed.uniquiz.conteudo.dominio.TipoDesafio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LeitorDeConteudo {

    private final ObjectMapper json;

    public LeitorDeConteudo(ObjectMapper json) {
        this.json = json;
    }

    public ConteudoDesafio ler(TipoDesafio tipo, String conteudoJson) {
        try {
            return json.readValue(conteudoJson, tipo.classeDoConteudo());
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("Conteúdo de desafio gravado em formato inválido", erro);
        }
    }

    public ConteudoDesafio converter(TipoDesafio tipo, JsonNode conteudo) {
        try {
            ConteudoDesafio convertido = json.treeToValue(conteudo, tipo.classeDoConteudo());
            convertido.validar();
            return convertido;
        } catch (JsonProcessingException | IllegalArgumentException erro) {
            throw new ErroDeNegocio("O conteúdo do desafio não está no formato esperado para esse tipo.");
        }
    }

    public RespostaDoColaborador lerResposta(TipoDesafio tipo, JsonNode resposta) {
        try {
            return json.treeToValue(resposta, tipo.classeDaResposta());
        } catch (JsonProcessingException | IllegalArgumentException erro) {
            throw new ErroDeNegocio("A resposta não está no formato esperado para esse desafio.");
        }
    }

    public JsonNode paraArvore(String conteudoJson) {
        try {
            return json.readTree(conteudoJson);
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("JSON gravado em formato inválido", erro);
        }
    }

    public String escrever(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException erro) {
            throw new IllegalStateException("Não foi possível converter para JSON", erro);
        }
    }
}

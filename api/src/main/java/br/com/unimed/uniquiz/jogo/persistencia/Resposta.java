package br.com.unimed.uniquiz.jogo.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Resposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tentativaId;
    private Long desafioId;
    private int desafioVersao;
    private String respostaJson;
    private boolean correta;
    private int pontos;
    private boolean segundaChance;
    private Instant respondidaEm;

    protected Resposta() {}

    public Resposta(
            Long tentativaId,
            Long desafioId,
            int desafioVersao,
            String respostaJson,
            boolean correta,
            int pontos,
            boolean segundaChance,
            Instant respondidaEm) {
        this.tentativaId = tentativaId;
        this.desafioId = desafioId;
        this.desafioVersao = desafioVersao;
        this.respostaJson = respostaJson;
        this.correta = correta;
        this.pontos = pontos;
        this.segundaChance = segundaChance;
        this.respondidaEm = respondidaEm;
    }

    public Long getDesafioId() {
        return desafioId;
    }

    public boolean isCorreta() {
        return correta;
    }

    public int getPontos() {
        return pontos;
    }

    public boolean isSegundaChance() {
        return segundaChance;
    }

    public boolean primeiraResposta() {
        return !segundaChance;
    }
}

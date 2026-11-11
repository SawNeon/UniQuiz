package br.com.unimed.uniquiz.jogo.persistencia;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.jogo.dominio.StatusDaTentativa;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.Instant;

@Entity
public class Tentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long colaboradorId;
    private Long licaoId;
    private Long temporadaId;
    private Instant iniciadaEm;
    private Instant concluidaEm;
    private int acertos;
    private int total;
    private Boolean perfeita;
    private Long tempoMs;
    private int percentualPontos;

    @Enumerated(EnumType.STRING)
    private StatusDaTentativa status;

    protected Tentativa() {}

    public Tentativa(
            Long colaboradorId,
            Long licaoId,
            Long temporadaId,
            Instant iniciadaEm,
            int total,
            int percentualPontos) {
        this.colaboradorId = colaboradorId;
        this.licaoId = licaoId;
        this.temporadaId = temporadaId;
        this.iniciadaEm = iniciadaEm;
        this.total = total;
        this.percentualPontos = percentualPontos;
        this.status = StatusDaTentativa.EM_ANDAMENTO;
    }

    public void exigirEmAndamento() {
        if (status != StatusDaTentativa.EM_ANDAMENTO) {
            throw new ErroDeNegocio("Esta lição já foi encerrada. Comece de novo pelo caminho.");
        }
    }

    public void registrarAcerto() {
        acertos++;
    }

    public void concluir(Instant agora, boolean semErros) {
        exigirEmAndamento();
        this.status = StatusDaTentativa.CONCLUIDA;
        this.concluidaEm = agora;
        this.perfeita = semErros;
        this.tempoMs = Duration.between(iniciadaEm, agora).toMillis();
    }

    public void abandonar() {
        this.status = StatusDaTentativa.ABANDONADA;
    }

    public Long getId() {
        return id;
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public Long getLicaoId() {
        return licaoId;
    }

    public Long getTemporadaId() {
        return temporadaId;
    }

    public int getAcertos() {
        return acertos;
    }

    public int getTotal() {
        return total;
    }

    public Long getTempoMs() {
        return tempoMs;
    }

    public int getPercentualPontos() {
        return percentualPontos;
    }
}

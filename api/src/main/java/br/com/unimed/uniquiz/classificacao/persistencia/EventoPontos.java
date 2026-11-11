package br.com.unimed.uniquiz.classificacao.persistencia;

import br.com.unimed.uniquiz.classificacao.dominio.MotivoDosPontos;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class EventoPontos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long colaboradorId;
    private Long temporadaId;
    private Long tentativaId;

    @Enumerated(EnumType.STRING)
    private MotivoDosPontos motivo;

    private int pontos;
    private Instant criadoEm;

    protected EventoPontos() {}

    public EventoPontos(
            Long colaboradorId,
            Long temporadaId,
            Long tentativaId,
            MotivoDosPontos motivo,
            int pontos,
            Instant criadoEm) {
        this.colaboradorId = colaboradorId;
        this.temporadaId = temporadaId;
        this.tentativaId = tentativaId;
        this.motivo = motivo;
        this.pontos = pontos;
        this.criadoEm = criadoEm;
    }
}

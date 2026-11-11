package br.com.unimed.uniquiz.trofeu.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;
import java.time.Instant;

@Entity
@IdClass(ColaboradorTrofeu.Chave.class)
public class ColaboradorTrofeu {

    public record Chave(Long colaboradorId, Long trofeuId) implements Serializable {}

    @Id
    private Long colaboradorId;

    @Id
    private Long trofeuId;

    private Instant conquistadoEm;

    protected ColaboradorTrofeu() {}

    public ColaboradorTrofeu(Long colaboradorId, Long trofeuId, Instant conquistadoEm) {
        this.colaboradorId = colaboradorId;
        this.trofeuId = trofeuId;
        this.conquistadoEm = conquistadoEm;
    }

    public Long getTrofeuId() {
        return trofeuId;
    }

    public Instant getConquistadoEm() {
        return conquistadoEm;
    }
}

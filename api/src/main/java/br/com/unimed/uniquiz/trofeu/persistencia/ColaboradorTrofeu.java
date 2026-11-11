package br.com.unimed.uniquiz.trofeu.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@IdClass(ColaboradorTrofeu.Chave.class)
public class ColaboradorTrofeu {

    public static class Chave implements Serializable {

        private Long colaboradorId;
        private Long trofeuId;

        protected Chave() {}

        public Chave(Long colaboradorId, Long trofeuId) {
            this.colaboradorId = colaboradorId;
            this.trofeuId = trofeuId;
        }

        @Override
        public boolean equals(Object outro) {
            return outro instanceof Chave chave
                    && Objects.equals(colaboradorId, chave.colaboradorId)
                    && Objects.equals(trofeuId, chave.trofeuId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(colaboradorId, trofeuId);
        }
    }

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

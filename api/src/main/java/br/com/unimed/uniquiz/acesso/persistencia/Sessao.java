package br.com.unimed.uniquiz.acesso.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Sessao {

    @Id
    private String tokenHash;

    private Long colaboradorId;
    private Long administradorId;
    private Instant criadaEm;
    private Instant expiraEm;

    protected Sessao() {}

    private Sessao(String tokenHash, Long colaboradorId, Long administradorId, Instant criadaEm, Instant expiraEm) {
        this.tokenHash = tokenHash;
        this.colaboradorId = colaboradorId;
        this.administradorId = administradorId;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public static Sessao deColaborador(String tokenHash, Long colaboradorId, Instant agora, Instant expiraEm) {
        return new Sessao(tokenHash, colaboradorId, null, agora, expiraEm);
    }

    public static Sessao deAdministrador(String tokenHash, Long administradorId, Instant agora, Instant expiraEm) {
        return new Sessao(tokenHash, null, administradorId, agora, expiraEm);
    }

    public boolean expirada(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public Long getAdministradorId() {
        return administradorId;
    }
}

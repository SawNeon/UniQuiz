package br.com.unimed.uniquiz.acesso.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.Instant;

@Entity
public class Administrador {

    public static final int TENTATIVAS_ANTES_DO_BLOQUEIO = 5;
    public static final Duration TEMPO_DE_BLOQUEIO = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario;
    private String senhaHash;
    private boolean ativo;
    private int tentativasFalhas;
    private Instant bloqueadoAte;

    protected Administrador() {}

    public Administrador(String usuario, String senhaHash) {
        this.usuario = usuario;
        this.senhaHash = senhaHash;
        this.ativo = true;
    }

    public boolean bloqueado(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public void registrarFalha(Instant agora) {
        tentativasFalhas++;
        if (tentativasFalhas >= TENTATIVAS_ANTES_DO_BLOQUEIO) {
            bloqueadoAte = agora.plus(TEMPO_DE_BLOQUEIO);
            tentativasFalhas = 0;
        }
    }

    public void registrarSucesso() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }
}

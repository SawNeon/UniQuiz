package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Licao {

    public static final int MINIMO_DE_DESAFIOS = 5;
    public static final int MAXIMO_DE_DESAFIOS = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long moduloId;
    private String titulo;
    private int ordem;

    @Enumerated(EnumType.STRING)
    private StatusPublicacao status;

    protected Licao() {}

    public Licao(Long moduloId, String titulo, int ordem) {
        this.moduloId = moduloId;
        this.ordem = ordem;
        this.status = StatusPublicacao.RASCUNHO;
        renomear(titulo);
    }

    public void renomear(String novoTitulo) {
        this.titulo = novoTitulo.strip();
    }

    public void mudarStatus(StatusPublicacao novoStatus) {
        this.status = novoStatus;
    }

    public void mover(int novaOrdem) {
        this.ordem = novaOrdem;
    }

    public boolean publicada() {
        return status == StatusPublicacao.PUBLICADO;
    }

    public Long getId() {
        return id;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getOrdem() {
        return ordem;
    }

    public StatusPublicacao getStatus() {
        return status;
    }
}

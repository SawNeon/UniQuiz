package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String descricao;
    private String materialEstudo;
    private int ordem;

    @Enumerated(EnumType.STRING)
    private StatusPublicacao status;

    protected Modulo() {}

    public Modulo(String titulo, String descricao, String materialEstudo, int ordem) {
        this.ordem = ordem;
        this.status = StatusPublicacao.RASCUNHO;
        atualizar(titulo, descricao, materialEstudo);
    }

    public void atualizar(String titulo, String descricao, String materialEstudo) {
        this.titulo = titulo.strip();
        this.descricao = descricao == null ? "" : descricao.strip();
        this.materialEstudo = materialEstudo == null || materialEstudo.isBlank() ? null : materialEstudo;
    }

    public void mudarStatus(StatusPublicacao novoStatus) {
        this.status = novoStatus;
    }

    public void mover(int novaOrdem) {
        this.ordem = novaOrdem;
    }

    public boolean publicado() {
        return status == StatusPublicacao.PUBLICADO;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getMaterialEstudo() {
        return materialEstudo;
    }

    public int getOrdem() {
        return ordem;
    }

    public StatusPublicacao getStatus() {
        return status;
    }
}

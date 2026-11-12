package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.QuantidadesPorNivel;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Licao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long moduloId;
    private String titulo;
    private int ordem;
    private int qtdFacil;
    private int qtdMedio;
    private int qtdDificil;

    @Enumerated(EnumType.STRING)
    private StatusPublicacao status;

    protected Licao() {}

    public Licao(Long moduloId, String titulo, int ordem, QuantidadesPorNivel quantidades) {
        this.moduloId = moduloId;
        this.ordem = ordem;
        this.status = StatusPublicacao.RASCUNHO;
        renomear(titulo);
        definirQuantidades(quantidades);
    }

    public void definirQuantidades(QuantidadesPorNivel quantidades) {
        this.qtdFacil = quantidades.facil();
        this.qtdMedio = quantidades.medio();
        this.qtdDificil = quantidades.dificil();
    }

    public QuantidadesPorNivel quantidades() {
        return new QuantidadesPorNivel(qtdFacil, qtdMedio, qtdDificil);
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

package br.com.unimed.uniquiz.trofeu.persistencia;

import br.com.unimed.uniquiz.trofeu.dominio.CodigoDoTrofeu;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity
public class Trofeu {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private CodigoDoTrofeu codigo;

    private String nome;
    private String descricao;
    private String icone;

    protected Trofeu() {}

    public Long getId() {
        return id;
    }

    public CodigoDoTrofeu getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getIcone() {
        return icone;
    }
}

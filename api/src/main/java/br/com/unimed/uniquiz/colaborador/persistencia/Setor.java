package br.com.unimed.uniquiz.colaborador.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long empresaId;
    private String nome;

    protected Setor() {}

    public Setor(Long empresaId, String nome) {
        this.empresaId = empresaId;
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getNome() {
        return nome;
    }
}

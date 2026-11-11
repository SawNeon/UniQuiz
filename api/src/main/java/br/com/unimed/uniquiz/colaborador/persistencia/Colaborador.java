package br.com.unimed.uniquiz.colaborador.persistencia;

import br.com.unimed.uniquiz.comum.Textos;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long empresaId;
    private Long setorId;
    private String nome;
    private String nomeNormalizado;
    private boolean ativo;
    private Instant criadoEm;

    protected Colaborador() {}

    public Colaborador(Long empresaId, Long setorId, String nome, Instant criadoEm) {
        this.empresaId = empresaId;
        this.setorId = setorId;
        this.ativo = true;
        this.criadoEm = criadoEm;
        renomear(nome);
    }

    public void renomear(String novoNome) {
        this.nome = Textos.limpar(novoNome);
        this.nomeNormalizado = Textos.normalizar(novoNome);
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public Long getSetorId() {
        return setorId;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeNormalizado() {
        return nomeNormalizado;
    }

    public boolean isAtivo() {
        return ativo;
    }
}

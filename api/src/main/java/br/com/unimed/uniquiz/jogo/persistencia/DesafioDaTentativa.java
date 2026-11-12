package br.com.unimed.uniquiz.jogo.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DesafioDaTentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tentativaId;
    private Long desafioId;
    private int posicao;

    protected DesafioDaTentativa() {}

    public DesafioDaTentativa(Long tentativaId, Long desafioId, int posicao) {
        this.tentativaId = tentativaId;
        this.desafioId = desafioId;
        this.posicao = posicao;
    }

    public Long getDesafioId() {
        return desafioId;
    }
}

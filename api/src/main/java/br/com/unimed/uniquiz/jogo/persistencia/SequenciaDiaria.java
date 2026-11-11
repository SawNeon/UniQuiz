package br.com.unimed.uniquiz.jogo.persistencia;

import br.com.unimed.uniquiz.jogo.dominio.SequenciaDeDias;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class SequenciaDiaria {

    @Id
    private Long colaboradorId;

    private int diasAtuais;
    private int maiorSequencia;
    private LocalDate ultimoDia;

    protected SequenciaDiaria() {}

    public SequenciaDiaria(Long colaboradorId, SequenciaDeDias sequencia) {
        this.colaboradorId = colaboradorId;
        aplicar(sequencia);
    }

    public SequenciaDeDias paraDominio() {
        return new SequenciaDeDias(diasAtuais, maiorSequencia, ultimoDia);
    }

    public void aplicar(SequenciaDeDias sequencia) {
        this.diasAtuais = sequencia.diasAtuais();
        this.maiorSequencia = sequencia.maiorSequencia();
        this.ultimoDia = sequencia.ultimoDia();
    }
}

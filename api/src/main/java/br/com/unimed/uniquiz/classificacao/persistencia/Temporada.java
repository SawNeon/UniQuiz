package br.com.unimed.uniquiz.classificacao.persistencia;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private LocalDate inicio;
    private LocalDate fim;
    private String premioTexto;

    protected Temporada() {}

    public Temporada(String nome, LocalDate inicio, LocalDate fim, String premioTexto) {
        atualizar(nome, inicio, fim, premioTexto);
    }

    public void atualizar(String nome, LocalDate inicio, LocalDate fim, String premioTexto) {
        if (fim.isBefore(inicio)) {
            throw new ErroDeNegocio("A data de fim precisa ser igual ou depois da data de início.");
        }
        this.nome = nome.strip();
        this.inicio = inicio;
        this.fim = fim;
        this.premioTexto = premioTexto == null || premioTexto.isBlank() ? null : premioTexto.strip();
    }

    public boolean sobrepoe(LocalDate outroInicio, LocalDate outroFim) {
        return !outroFim.isBefore(inicio) && !outroInicio.isAfter(fim);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public String getPremioTexto() {
        return premioTexto;
    }
}

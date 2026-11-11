package br.com.unimed.uniquiz.certificado.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private Long colaboradorId;
    private Long moduloId;
    private String nomeColaborador;
    private String tituloModulo;
    private Instant emitidoEm;

    protected Certificado() {}

    public Certificado(
            String codigo,
            Long colaboradorId,
            Long moduloId,
            String nomeColaborador,
            String tituloModulo,
            Instant emitidoEm) {
        this.codigo = codigo;
        this.colaboradorId = colaboradorId;
        this.moduloId = moduloId;
        this.nomeColaborador = nomeColaborador;
        this.tituloModulo = tituloModulo;
        this.emitidoEm = emitidoEm;
    }

    public String getCodigo() {
        return codigo;
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public String getNomeColaborador() {
        return nomeColaborador;
    }

    public String getTituloModulo() {
        return tituloModulo;
    }

    public Instant getEmitidoEm() {
        return emitidoEm;
    }
}

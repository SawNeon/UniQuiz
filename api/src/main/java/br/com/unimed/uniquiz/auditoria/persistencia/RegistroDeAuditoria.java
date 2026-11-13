package br.com.unimed.uniquiz.auditoria.persistencia;

import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.ResultadoDaAcao;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
public class RegistroDeAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant momento;

    @Enumerated(EnumType.STRING)
    private TipoDeAtor atorTipo;

    private Long atorId;
    private String atorDescricao;
    private String acao;
    private String alvo;

    @Enumerated(EnumType.STRING)
    private ResultadoDaAcao resultado;

    private String detalhe;
    private String ip;

    protected RegistroDeAuditoria() {}

    public RegistroDeAuditoria(Instant momento, OcorrenciaDeAuditoria ocorrencia, String ip) {
        this.momento = momento;
        this.atorTipo = ocorrencia.ator();
        this.atorId = ocorrencia.atorId();
        this.atorDescricao = ocorrencia.atorDescricao();
        this.acao = ocorrencia.acao().name();
        this.alvo = ocorrencia.alvo();
        this.resultado = ocorrencia.resultado();
        this.detalhe = ocorrencia.detalhe();
        this.ip = ip;
    }
}

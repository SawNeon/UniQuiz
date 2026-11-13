package br.com.unimed.uniquiz.auditoria.aplicacao;

import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.persistencia.RegistroDeAuditoria;
import br.com.unimed.uniquiz.auditoria.persistencia.RegistroDeAuditoriaRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeAuditoria {

    private final RegistroDeAuditoriaRepository registros;
    private final OrigemDaRequisicao origem;
    private final Clock relogio;

    public ServicoDeAuditoria(RegistroDeAuditoriaRepository registros, OrigemDaRequisicao origem, Clock relogio) {
        this.registros = registros;
        this.origem = origem;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(OcorrenciaDeAuditoria ocorrencia) {
        registros.save(new RegistroDeAuditoria(
                relogio.instant(), ocorrencia, origem.ipAtual().orElse(null)));
    }
}

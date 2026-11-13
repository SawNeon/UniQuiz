package br.com.unimed.uniquiz.auditoria.persistencia;

import org.springframework.data.repository.Repository;

public interface RegistroDeAuditoriaRepository extends Repository<RegistroDeAuditoria, Long> {

    RegistroDeAuditoria save(RegistroDeAuditoria registro);
}

package br.com.unimed.uniquiz.certificado.persistencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    Optional<Certificado> findByCodigo(String codigo);

    Optional<Certificado> findByColaboradorIdAndModuloId(Long colaboradorId, Long moduloId);

    List<Certificado> findByColaboradorIdOrderByEmitidoEmDesc(Long colaboradorId);

    boolean existsByCodigo(String codigo);
}

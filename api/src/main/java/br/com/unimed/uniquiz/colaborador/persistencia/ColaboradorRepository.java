package br.com.unimed.uniquiz.colaborador.persistencia;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Colaborador c where c.id = :id")
    Optional<Colaborador> buscarTravadoPorId(@Param("id") Long id);

    List<Colaborador> findTop10BySetorIdAndAtivoTrueAndNomeNormalizadoContainingOrderByNomeAsc(
            Long setorId, String trecho);

    List<Colaborador> findTop200ByNomeNormalizadoContainingOrderByNomeAsc(String trecho);

    boolean existsByEmpresaIdAndSetorIdAndNomeNormalizado(Long empresaId, Long setorId, String nomeNormalizado);

    boolean existsByEmpresaIdAndSetorIdAndNomeNormalizadoAndIdNot(
            Long empresaId, Long setorId, String nomeNormalizado, Long id);
}

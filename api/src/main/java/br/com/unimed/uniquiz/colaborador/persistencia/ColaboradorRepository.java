package br.com.unimed.uniquiz.colaborador.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

    List<Colaborador> findBySetorIdAndAtivoTrueOrderByNomeAsc(Long setorId);

    List<Colaborador> findTop200ByNomeNormalizadoContainingOrderByNomeAsc(String trecho);

    boolean existsByEmpresaIdAndSetorIdAndNomeNormalizadoAndIdNot(
            Long empresaId, Long setorId, String nomeNormalizado, Long id);
}

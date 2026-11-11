package br.com.unimed.uniquiz.jogo.persistencia;

import br.com.unimed.uniquiz.jogo.dominio.StatusDaTentativa;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TentativaRepository extends JpaRepository<Tentativa, Long> {

    Optional<Tentativa> findByIdAndColaboradorId(Long id, Long colaboradorId);

    List<Tentativa> findByColaboradorIdAndLicaoIdAndStatus(Long colaboradorId, Long licaoId, StatusDaTentativa status);

    long countByColaboradorIdAndLicaoIdAndStatus(Long colaboradorId, Long licaoId, StatusDaTentativa status);

    boolean existsByColaboradorIdAndStatusAndConcluidaEmBetween(
            Long colaboradorId, StatusDaTentativa status, Instant inicio, Instant fim);

    @Query("select distinct t.licaoId from Tentativa t where t.colaboradorId = :colaboradorId and t.status = :status")
    Set<Long> licoesComStatus(Long colaboradorId, StatusDaTentativa status);
}

package br.com.unimed.uniquiz.classificacao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventoPontosRepository extends JpaRepository<EventoPontos, Long> {

    @Query("select coalesce(sum(e.pontos), 0) from EventoPontos e where e.colaboradorId = :colaboradorId")
    int totalDoColaborador(Long colaboradorId);

    @Query("select coalesce(sum(e.pontos), 0) from EventoPontos e "
            + "where e.colaboradorId = :colaboradorId and e.temporadaId = :temporadaId")
    int totalNaTemporada(Long colaboradorId, Long temporadaId);
}

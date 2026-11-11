package br.com.unimed.uniquiz.classificacao.persistencia;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findAllByOrderByInicioDesc();

    List<Temporada> findByInicioLessThanEqualAndFimGreaterThanEqualOrderByInicioDesc(LocalDate dia, LocalDate mesmoDia);
}

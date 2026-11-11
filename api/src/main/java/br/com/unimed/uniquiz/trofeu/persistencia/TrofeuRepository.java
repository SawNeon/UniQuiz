package br.com.unimed.uniquiz.trofeu.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrofeuRepository extends JpaRepository<Trofeu, Long> {

    List<Trofeu> findAllByOrderByIdAsc();
}

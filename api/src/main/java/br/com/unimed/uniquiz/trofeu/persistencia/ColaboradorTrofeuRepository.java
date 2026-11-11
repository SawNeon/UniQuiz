package br.com.unimed.uniquiz.trofeu.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColaboradorTrofeuRepository extends JpaRepository<ColaboradorTrofeu, ColaboradorTrofeu.Chave> {

    List<ColaboradorTrofeu> findByColaboradorId(Long colaboradorId);
}

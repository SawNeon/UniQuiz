package br.com.unimed.uniquiz.jogo.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    List<Resposta> findByTentativaIdOrderByIdAsc(Long tentativaId);
}

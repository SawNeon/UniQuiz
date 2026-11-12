package br.com.unimed.uniquiz.jogo.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesafioDaTentativaRepository extends JpaRepository<DesafioDaTentativa, Long> {

    List<DesafioDaTentativa> findByTentativaIdOrderByPosicaoAsc(Long tentativaId);
}

package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    List<Modulo> findAllByOrderByOrdemAsc();

    List<Modulo> findByStatusOrderByOrdemAsc(StatusPublicacao status);

    @Query("select coalesce(max(m.ordem), 0) from Modulo m")
    int maiorOrdem();
}

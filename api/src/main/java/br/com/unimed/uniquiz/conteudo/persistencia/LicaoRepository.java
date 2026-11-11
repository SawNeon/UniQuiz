package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LicaoRepository extends JpaRepository<Licao, Long> {

    List<Licao> findByModuloIdOrderByOrdemAsc(Long moduloId);

    List<Licao> findByModuloIdInAndStatusOrderByOrdemAsc(Collection<Long> modulosIds, StatusPublicacao status);

    long countByModuloIdAndStatus(Long moduloId, StatusPublicacao status);

    @Query("select coalesce(max(l.ordem), 0) from Licao l where l.moduloId = :moduloId")
    int maiorOrdem(Long moduloId);
}

package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DesafioRepository extends JpaRepository<Desafio, Long> {

    List<Desafio> findByLicaoIdOrderByOrdemAsc(Long licaoId);

    List<Desafio> findByLicaoIdAndStatusOrderByOrdemAsc(Long licaoId, StatusPublicacao status);

    long countByLicaoIdAndStatus(Long licaoId, StatusPublicacao status);

    @Query("select coalesce(max(d.ordem), 0) from Desafio d where d.licaoId = :licaoId")
    int maiorOrdem(Long licaoId);
}

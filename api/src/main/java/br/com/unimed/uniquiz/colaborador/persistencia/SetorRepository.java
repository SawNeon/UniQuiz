package br.com.unimed.uniquiz.colaborador.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    List<Setor> findByEmpresaIdOrderByNomeAsc(Long empresaId);
}

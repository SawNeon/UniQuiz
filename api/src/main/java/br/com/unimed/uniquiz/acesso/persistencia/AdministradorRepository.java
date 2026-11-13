package br.com.unimed.uniquiz.acesso.persistencia;

import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    long countByPerfilAndAtivoTrue(PerfilDoPainel perfil);

    List<Administrador> findAllByOrderByNomeAsc();
}

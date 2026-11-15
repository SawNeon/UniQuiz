package br.com.unimed.uniquiz.acesso.persistencia;

import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByUsuario(String usuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Administrador a where a.usuario = :usuario")
    Optional<Administrador> buscarTravadoPorUsuario(@Param("usuario") String usuario);

    boolean existsByUsuario(String usuario);

    long countByPerfilAndAtivoTrue(PerfilDoPainel perfil);

    List<Administrador> findAllByOrderByNomeAsc();
}

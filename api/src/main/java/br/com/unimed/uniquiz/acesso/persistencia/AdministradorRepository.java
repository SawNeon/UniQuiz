package br.com.unimed.uniquiz.acesso.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByUsuario(String usuario);
}

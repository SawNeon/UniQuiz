package br.com.unimed.uniquiz.acesso.persistencia;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SessaoRepository extends JpaRepository<Sessao, String> {

    @Modifying
    @Query("delete from Sessao s where s.expiraEm <= :agora")
    int apagarExpiradas(Instant agora);

    @Modifying
    @Query("delete from Sessao s where s.administradorId = :administradorId")
    int apagarDoAdministrador(Long administradorId);

    @Modifying
    @Query("delete from Sessao s where s.administradorId = :administradorId and s.tokenHash <> :tokenHashMantido")
    int apagarDoAdministradorExceto(Long administradorId, String tokenHashMantido);

    List<Sessao> findByAdministradorIdOrderByUltimoUsoEmDesc(Long administradorId);
}

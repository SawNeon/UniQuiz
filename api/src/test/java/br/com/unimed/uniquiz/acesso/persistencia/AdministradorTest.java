package br.com.unimed.uniquiz.acesso.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdministradorTest {

    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");

    private static Administrador administrador() {
        return new Administrador("admin", "Admin", "hash", PerfilDoPainel.ADMINISTRADOR, Set.of(), false, AGORA);
    }

    @Test
    void bloqueiaNaQuintaFalhaPorQuinzeMinutos() {
        Administrador administrador = administrador();

        for (int falha = 1; falha < Administrador.TENTATIVAS_ANTES_DO_BLOQUEIO; falha++) {
            administrador.registrarFalha(AGORA);
            assertThat(administrador.bloqueado(AGORA)).isFalse();
        }
        administrador.registrarFalha(AGORA);

        assertThat(administrador.bloqueado(AGORA)).isTrue();
        assertThat(administrador.bloqueado(
                        AGORA.plus(Administrador.TEMPO_DE_BLOQUEIO).minusSeconds(1)))
                .isTrue();
        assertThat(administrador.bloqueado(AGORA.plus(Administrador.TEMPO_DE_BLOQUEIO)))
                .isFalse();
    }

    @Test
    void acessoZeraAsFalhasERegistraOMomento() {
        Administrador administrador = administrador();
        for (int falha = 1; falha < Administrador.TENTATIVAS_ANTES_DO_BLOQUEIO; falha++) {
            administrador.registrarFalha(AGORA);
        }

        administrador.registrarAcesso(AGORA);
        administrador.registrarFalha(AGORA);

        assertThat(administrador.bloqueado(AGORA)).isFalse();
        assertThat(administrador.getUltimoAcessoEm()).isEqualTo(AGORA);
    }

    @Test
    void senhaProvisoriaExigeTrocaEDesbloqueia() {
        Administrador administrador = administrador();
        for (int falha = 0; falha < Administrador.TENTATIVAS_ANTES_DO_BLOQUEIO; falha++) {
            administrador.registrarFalha(AGORA);
        }

        administrador.definirSenhaProvisoria("provisoria", AGORA);

        assertThat(administrador.isPrecisaTrocarSenha()).isTrue();
        assertThat(administrador.bloqueado(AGORA)).isFalse();

        administrador.trocarSenha("nova", AGORA);

        assertThat(administrador.isPrecisaTrocarSenha()).isFalse();
        assertThat(administrador.getSenhaHash()).isEqualTo("nova");
    }

    @Test
    void soOGestorGuardaSetores() {
        Administrador administrador = administrador();

        administrador.alterarPerfil(PerfilDoPainel.GESTOR, Set.of(3L, 4L));
        assertThat(administrador.getSetores()).containsExactlyInAnyOrder(3L, 4L);
        assertThat(administrador.escopo().permite(3L)).isTrue();
        assertThat(administrador.escopo().permite(9L)).isFalse();

        administrador.alterarPerfil(PerfilDoPainel.AUDITOR, Set.of(3L));
        assertThat(administrador.getSetores()).isEmpty();
        assertThat(administrador.escopo().permite(9L)).isTrue();
    }

    @Test
    void contaComoAdministradorAtivoSoQuandoAtivaEComEssePerfil() {
        Administrador administrador = administrador();
        assertThat(administrador.administradorAtivo()).isTrue();

        administrador.desativar();
        assertThat(administrador.administradorAtivo()).isFalse();

        administrador.reativar();
        administrador.alterarPerfil(PerfilDoPainel.AUDITOR, Set.of());
        assertThat(administrador.administradorAtivo()).isFalse();
    }
}

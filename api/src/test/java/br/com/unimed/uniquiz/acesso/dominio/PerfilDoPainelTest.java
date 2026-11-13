package br.com.unimed.uniquiz.acesso.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PerfilDoPainelTest {

    @Test
    void administradorPodeTudo() {
        assertThat(PerfilDoPainel.ADMINISTRADOR.permissoes()).isEqualTo(EnumSet.allOf(Permissao.class));
        assertThat(PerfilDoPainel.ADMINISTRADOR.limitadoAosSetores()).isFalse();
    }

    @Test
    void gestorSoVeEExportaRelatoriosDosSeusSetores() {
        assertThat(PerfilDoPainel.GESTOR.permissoes())
                .containsExactlyInAnyOrder(Permissao.VER_RELATORIOS, Permissao.EXPORTAR_RELATORIOS);
        assertThat(PerfilDoPainel.GESTOR.limitadoAosSetores()).isTrue();
    }

    @Test
    void auditorLeTudoSemAlterarNada() {
        assertThat(PerfilDoPainel.AUDITOR.permissoes())
                .containsExactlyInAnyOrder(
                        Permissao.VER_RELATORIOS, Permissao.EXPORTAR_RELATORIOS, Permissao.VER_AUDITORIA);
        assertThat(PerfilDoPainel.AUDITOR.permissoes())
                .noneMatch(permissao -> permissao.name().startsWith("GERIR_"));
    }

    @Test
    void permissaoViraAutoridadeDoSpring() {
        assertThat(Permissao.VER_AUDITORIA.autoridade()).isEqualTo("PERM_VER_AUDITORIA");
    }

    @Test
    void escopoLimitadoSoPermiteOsSetoresAtribuidos() {
        EscopoDeSetores doGestor = EscopoDeSetores.para(PerfilDoPainel.GESTOR, Set.of(1L, 2L));
        EscopoDeSetores doAuditor = EscopoDeSetores.para(PerfilDoPainel.AUDITOR, Set.of(1L));

        assertThat(doGestor.permite(1L)).isTrue();
        assertThat(doGestor.permite(3L)).isFalse();
        assertThat(doAuditor.irrestrito()).isTrue();
        assertThat(doAuditor.permite(3L)).isTrue();
    }
}

package br.com.unimed.uniquiz.acesso.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import org.junit.jupiter.api.Test;

class UsuarioLogadoTest {

    @Test
    void colaboradorSoTemOPapelDeColaborador() {
        assertThat(UsuarioLogado.colaborador(1L).autoridades()).containsExactly("ROLE_COLABORADOR");
        assertThat(UsuarioLogado.colaborador(1L).permissoes()).isEmpty();
    }

    @Test
    void usuarioDoPainelLevaOPapelEAsPermissoesDoPerfil() {
        UsuarioLogado auditor = UsuarioLogado.doPainel(2L, PerfilDoPainel.AUDITOR, EscopoDeSetores.todos(), false);

        assertThat(auditor.autoridades())
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN", "PERM_VER_RELATORIOS", "PERM_EXPORTAR_RELATORIOS", "PERM_VER_AUDITORIA");
    }

    @Test
    void trocaDeSenhaPendenteTiraTodasAsPermissoes() {
        UsuarioLogado pendente =
                UsuarioLogado.doPainel(3L, PerfilDoPainel.ADMINISTRADOR, EscopoDeSetores.todos(), true);

        assertThat(pendente.permissoes()).isEmpty();
        assertThat(pendente.autoridades()).containsExactly("ROLE_ADMIN");
    }
}

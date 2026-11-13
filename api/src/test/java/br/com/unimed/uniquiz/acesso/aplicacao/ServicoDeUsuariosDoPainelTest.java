package br.com.unimed.uniquiz.acesso.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel.DadosDoUsuario;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel.SenhaProvisoria;
import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class ServicoDeUsuariosDoPainelTest {

    private static final Instant AGORA = Instant.parse("2026-11-13T12:00:00Z");
    private static final long EU = 1L;
    private static final long OUTRO = 2L;

    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final ServicoDeSessao sessoes = mock(ServicoDeSessao.class);
    private final ServicoDeColaboradores colaboradores = mock(ServicoDeColaboradores.class);
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final ServicoDeUsuariosDoPainel servico = new ServicoDeUsuariosDoPainel(
            administradores,
            sessoes,
            colaboradores,
            new GeradorDeSenhaProvisoria(),
            codificador,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    private Administrador eu;
    private Administrador outro;

    @BeforeEach
    void prepararContas() {
        eu = conta(EU, "eu", PerfilDoPainel.ADMINISTRADOR);
        outro = conta(OUTRO, "outro", PerfilDoPainel.ADMINISTRADOR);
        when(administradores.findById(EU)).thenReturn(Optional.of(eu));
        when(administradores.findById(OUTRO)).thenReturn(Optional.of(outro));
        when(administradores.countByPerfilAndAtivoTrue(PerfilDoPainel.ADMINISTRADOR))
                .thenReturn(2L);
        when(administradores.save(any())).thenAnswer(chamada -> {
            Administrador salvo = chamada.getArgument(0);
            ReflectionTestUtils.setField(salvo, "id", 99L);
            return salvo;
        });
    }

    private static Administrador conta(long id, String usuario, PerfilDoPainel perfil) {
        Administrador conta = new Administrador(usuario, usuario, "hash", perfil, Set.of(), false, AGORA);
        ReflectionTestUtils.setField(conta, "id", id);
        return conta;
    }

    @Test
    void criaComLoginNormalizadoESenhaProvisoriaQueExigeTroca() {
        SenhaProvisoria criada =
                servico.criar("  Maria.Silva ", new DadosDoUsuario("Maria", PerfilDoPainel.AUDITOR, Set.of()));

        assertThat(criada.usuario()).isEqualTo("maria.silva");
        assertThat(criada.senha()).hasSize(19);
        verify(administradores).save(any());
    }

    @Test
    void recusaLoginRepetidoOuForaDoFormato() {
        when(administradores.existsByUsuario("maria")).thenReturn(true);
        DadosDoUsuario dados = new DadosDoUsuario("Maria", PerfilDoPainel.AUDITOR, Set.of());

        assertThatThrownBy(() -> servico.criar("maria", dados))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("Já existe");
        assertThatThrownBy(() -> servico.criar("maria silva", dados))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("O login precisa");
    }

    @Test
    void gestorPrecisaDeSetoresQueExistam() {
        assertThatThrownBy(() -> servico.criar("gestor", new DadosDoUsuario("Gestor", PerfilDoPainel.GESTOR, Set.of())))
                .hasMessage("Escolha ao menos um setor para o Gestor.");

        when(colaboradores.setoresExistem(Set.of(5L))).thenReturn(false);
        assertThatThrownBy(
                        () -> servico.criar("gestor", new DadosDoUsuario("Gestor", PerfilDoPainel.GESTOR, Set.of(5L))))
                .hasMessage("Um dos setores escolhidos não existe mais.");
    }

    @Test
    void ninguemDesativaNemRebaixaASiMesmo() {
        assertThatThrownBy(() -> servico.desativar(EU, EU)).hasMessage("Você não pode desativar a própria conta.");
        assertThatThrownBy(() -> servico.editar(EU, new DadosDoUsuario("Eu", PerfilDoPainel.AUDITOR, Set.of()), EU))
                .hasMessage("Você não pode tirar o próprio perfil de Administrador.");
        assertThatThrownBy(() -> servico.redefinirSenha(EU, EU)).hasMessageContaining("Minha conta");
    }

    @Test
    void sempreSobraUmAdministradorAtivo() {
        when(administradores.countByPerfilAndAtivoTrue(PerfilDoPainel.ADMINISTRADOR))
                .thenReturn(1L);

        assertThatThrownBy(() -> servico.desativar(OUTRO, EU))
                .hasMessage("É preciso manter ao menos um Administrador ativo.");
        assertThat(outro.isAtivo()).isTrue();
    }

    @Test
    void desativarDerrubaAsSessoes() {
        servico.desativar(OUTRO, EU);

        assertThat(outro.isAtivo()).isFalse();
        verify(sessoes).encerrarTodasDoAdministrador(OUTRO);
    }

    @Test
    void redefinirSenhaExigeTrocaEDerrubaAsSessoes() {
        SenhaProvisoria nova = servico.redefinirSenha(OUTRO, EU);

        assertThat(outro.isPrecisaTrocarSenha()).isTrue();
        assertThat(codificador.matches(nova.senha(), outro.getSenhaHash())).isTrue();
        verify(sessoes).encerrarTodasDoAdministrador(OUTRO);
    }

    @Test
    void contaComTrocaPendenteNaoMostraPermissoes() {
        outro.definirSenhaProvisoria("hash", AGORA);

        assertThat(servico.conta(OUTRO).permissoes()).isEmpty();
        assertThat(servico.conta(OUTRO).precisaTrocarSenha()).isTrue();
        assertThat(servico.conta(EU).permissoes()).isNotEmpty();
        verify(sessoes, never()).encerrarTodasDoAdministrador(any());
    }
}

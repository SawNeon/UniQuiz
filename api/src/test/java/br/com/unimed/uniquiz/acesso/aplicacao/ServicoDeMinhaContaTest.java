package br.com.unimed.uniquiz.acesso.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ServicoDeMinhaContaTest {

    private static final Instant AGORA = Instant.parse("2026-11-13T12:00:00Z");
    private static final long ID = 1L;
    private static final String SENHA_ATUAL = "provisoria-de-teste-1";
    private static final String NOVA_SENHA = "pao de queijo no forno";

    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final ServicoDeSessao sessoes = mock(ServicoDeSessao.class);
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final ServicoDeMinhaConta servico =
            new ServicoDeMinhaConta(administradores, sessoes, codificador, Clock.fixed(AGORA, ZoneOffset.UTC));

    private Administrador conta;

    @BeforeEach
    void prepararConta() {
        conta = new Administrador(
                "maria", "Maria", codificador.encode(SENHA_ATUAL), PerfilDoPainel.AUDITOR, Set.of(), true, AGORA);
        when(administradores.findById(ID)).thenReturn(Optional.of(conta));
    }

    @Test
    void trocaASenhaLiberaOPainelEDerrubaAsOutrasSessoes() {
        servico.trocarSenha(ID, SENHA_ATUAL, NOVA_SENHA, "token-atual");

        assertThat(codificador.matches(NOVA_SENHA, conta.getSenhaHash())).isTrue();
        assertThat(conta.isPrecisaTrocarSenha()).isFalse();
        verify(sessoes).encerrarOutrasDoAdministrador(ID, "token-atual");
    }

    @Test
    void senhaAtualErradaContaComoFalhaEAteBloqueia() {
        for (int falha = 0; falha < Administrador.TENTATIVAS_ANTES_DO_BLOQUEIO; falha++) {
            assertThatThrownBy(() -> servico.trocarSenha(ID, "errada-errada", NOVA_SENHA, "t"))
                    .hasMessage("A senha atual não confere.");
        }

        assertThatThrownBy(() -> servico.trocarSenha(ID, SENHA_ATUAL, NOVA_SENHA, "t"))
                .hasMessageContaining("bloqueado");
        verify(sessoes, never()).encerrarOutrasDoAdministrador(ID, "t");
    }

    @Test
    void novaSenhaPrecisaSeguirAPoliticaESerDiferente() {
        assertThatThrownBy(() -> servico.trocarSenha(ID, SENHA_ATUAL, "curta", "t"))
                .hasMessageContaining("12 caracteres");
        assertThatThrownBy(() -> servico.trocarSenha(ID, SENHA_ATUAL, SENHA_ATUAL, "t"))
                .hasMessageContaining("diferente da atual");
        assertThatThrownBy(() -> servico.trocarSenha(ID, SENHA_ATUAL, "a senha da maria aqui", "t"))
                .hasMessageContaining("nome de usuário");
    }
}

package br.com.unimed.uniquiz.colaborador.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.unimed.uniquiz.colaborador.persistencia.Colaborador;
import br.com.unimed.uniquiz.colaborador.persistencia.ColaboradorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ServicoDePinTest {

    private static final long ID = 7L;
    private static final Instant AGORA = Instant.parse("2026-11-12T12:00:00Z");

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final RelogioAjustavel relogio = new RelogioAjustavel(AGORA);
    private final ColaboradorRepository colaboradores = mock(ColaboradorRepository.class);
    private final ServicoDePin servico = new ServicoDePin(colaboradores, codificador, relogio);

    private Colaborador ana;

    @BeforeEach
    void cadastrarAna() {
        ana = new Colaborador(1L, 1L, "Ana Exemplo", AGORA);
        when(colaboradores.buscarTravadoPorId(ID)).thenReturn(Optional.of(ana));
        when(colaboradores.findById(ID)).thenReturn(Optional.of(ana));
    }

    @Test
    void primeiroAcessoCriaOPinEGuardaSoOHash() {
        servico.verificarOuCriar(ID, "482");

        assertThat(ana.temPin()).isTrue();
        assertThat(ana.getPinHash()).doesNotContain("482");
        assertThat(codificador.matches("482", ana.getPinHash())).isTrue();
    }

    @Test
    void depoisDoPrimeiroAcessoSoOMesmoPinEntra() {
        servico.verificarOuCriar(ID, "482");

        assertThatCode(() -> servico.verificarOuCriar(ID, "482")).doesNotThrowAnyException();
        assertThatThrownBy(() -> servico.verificarOuCriar(ID, "111"))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessage("PIN incorreto.");
    }

    @Test
    void cincoPinsErradosBloqueiamMesmoParaOPinCerto() {
        servico.verificarOuCriar(ID, "482");
        errarVezes(5);

        assertThatThrownBy(() -> servico.verificarOuCriar(ID, "482"))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    void quatroErrosAindaNaoBloqueiam() {
        servico.verificarOuCriar(ID, "482");
        errarVezes(4);

        assertThatCode(() -> servico.verificarOuCriar(ID, "482")).doesNotThrowAnyException();
    }

    @Test
    void depoisDeQuinzeMinutosODesbloqueioEAutomatico() {
        servico.verificarOuCriar(ID, "482");
        errarVezes(5);

        relogio.avancar(Duration.ofMinutes(15));

        assertThatCode(() -> servico.verificarOuCriar(ID, "482")).doesNotThrowAnyException();
    }

    @Test
    void acertoZeraAContagemDeErros() {
        servico.verificarOuCriar(ID, "482");
        errarVezes(4);
        servico.verificarOuCriar(ID, "482");
        errarVezes(4);

        assertThatCode(() -> servico.verificarOuCriar(ID, "482")).doesNotThrowAnyException();
    }

    @Test
    void redefinirApagaOPinEDestravaAPessoa() {
        servico.verificarOuCriar(ID, "482");
        errarVezes(5);

        servico.redefinir(ID);

        assertThat(ana.temPin()).isFalse();
        assertThatCode(() -> servico.verificarOuCriar(ID, "777")).doesNotThrowAnyException();
        assertThatThrownBy(() -> servico.verificarOuCriar(ID, "482")).isInstanceOf(ErroDeNegocio.class);
    }

    @Test
    void colaboradorInativoNaoEntra() {
        ana.desativar();

        assertThatThrownBy(() -> servico.verificarOuCriar(ID, "482"))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("ativos");
        assertThat(ana.temPin()).isFalse();
    }

    @Test
    void colaboradorQueNaoExisteNaoEntraNemTemPinRedefinido() {
        when(colaboradores.buscarTravadoPorId(99L)).thenReturn(Optional.empty());
        when(colaboradores.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.verificarOuCriar(99L, "482")).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> servico.redefinir(99L)).isInstanceOf(RecursoNaoEncontrado.class);
    }

    private void errarVezes(int vezes) {
        for (int tentativa = 0; tentativa < vezes; tentativa++) {
            assertThatThrownBy(() -> servico.verificarOuCriar(ID, "000")).isInstanceOf(ErroDeNegocio.class);
        }
    }

    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }
}

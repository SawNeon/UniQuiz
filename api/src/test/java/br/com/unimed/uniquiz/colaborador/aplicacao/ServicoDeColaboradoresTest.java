package br.com.unimed.uniquiz.colaborador.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores.ColaboradorParaAdmin;
import br.com.unimed.uniquiz.colaborador.persistencia.Colaborador;
import br.com.unimed.uniquiz.colaborador.persistencia.ColaboradorRepository;
import br.com.unimed.uniquiz.colaborador.persistencia.Empresa;
import br.com.unimed.uniquiz.colaborador.persistencia.EmpresaRepository;
import br.com.unimed.uniquiz.colaborador.persistencia.Setor;
import br.com.unimed.uniquiz.colaborador.persistencia.SetorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ServicoDeColaboradoresTest {

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final SetorRepository setores = mock(SetorRepository.class);
    private final ColaboradorRepository colaboradores = mock(ColaboradorRepository.class);
    private final Clock relogio = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);
    private final ServicoDeColaboradores servico =
            new ServicoDeColaboradores(empresas, setores, colaboradores, codificador, relogio);

    @BeforeEach
    void prepararRepositorios() {
        when(empresas.findAll()).thenReturn(List.of(new Empresa("Unimed Exemplo")));
        when(setores.findByEmpresaIdOrderByNomeAsc(any()))
                .thenReturn(List.of(new Setor(1L, "Tecnologia da Informação")));
        when(colaboradores.existsByEmpresaIdAndSetorIdAndNomeNormalizado(any(), any(), anyString()))
                .thenReturn(false);
        when(empresas.save(any(Empresa.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(setores.save(any(Setor.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(colaboradores.save(any(Colaborador.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    void comPinOColaboradorJaNasceComEleGuardadoSoComoHash() {
        ColaboradorParaAdmin cadastrado =
                servico.cadastrar("Eva Nova", "Unimed Exemplo", "Tecnologia da Informação", "246");

        assertThat(cadastrado.temPin()).isTrue();
        Colaborador salvo = colaboradorSalvo();
        assertThat(salvo.getPinHash()).doesNotContain("246");
        assertThat(codificador.matches("246", salvo.getPinHash())).isTrue();
    }

    @Test
    void semPinOColaboradorCriaOPinNoPrimeiroAcesso() {
        ColaboradorParaAdmin cadastrado =
                servico.cadastrar("Eva Nova", "Unimed Exemplo", "Tecnologia da Informação", null);

        assertThat(cadastrado.temPin()).isFalse();
        assertThat(colaboradorSalvo().temPin()).isFalse();
    }

    @Test
    void usaEmpresaESetorQueJaExistemIgnorandoMaiusculasEAcentos() {
        servico.cadastrar("Eva Nova", "UNIMED exemplo", "tecnologia da informacao", null);

        verify(empresas, never()).save(any());
        verify(setores, never()).save(any());
    }

    @Test
    void criaEmpresaESetorQueNaoExistem() {
        servico.cadastrar("Eva Nova", "Outra Empresa", "Novo Setor", null);

        verify(empresas).save(any(Empresa.class));
        verify(setores).save(any(Setor.class));
    }

    @Test
    void recusaNomeRepetidoNoMesmoSetorSemGravarNada() {
        when(colaboradores.existsByEmpresaIdAndSetorIdAndNomeNormalizado(any(), any(), anyString()))
                .thenReturn(true);

        assertThatThrownBy(() -> servico.cadastrar("Eva Nova", "Unimed Exemplo", "Tecnologia da Informação", "246"))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("Já existe uma pessoa");
        verify(colaboradores, never()).save(any(Colaborador.class));
    }

    @Test
    void buscaDaEntradaExigeTresLetrasEIgnoraAcentos() {
        when(colaboradores.findTop10BySetorIdAndAtivoTrueAndNomeNormalizadoContainingOrderByNomeAsc(2L, "JOA"))
                .thenReturn(List.of(new Colaborador(1L, 2L, "João Ávila", Instant.parse("2026-11-12T12:00:00Z"))));

        assertThat(servico.buscarNaEntrada(2L, "  joã "))
                .extracting(ServicoDeColaboradores.ColaboradorDaEntrada::nome)
                .containsExactly("João Ávila");
        assertThatThrownBy(() -> servico.buscarNaEntrada(2L, "jo"))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessage("Digite pelo menos 3 letras do seu nome.");
        assertThatThrownBy(() -> servico.buscarNaEntrada(2L, "a 1 2 3")).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> servico.buscarNaEntrada(2L, null)).isInstanceOf(ErroDeNegocio.class);
    }

    private Colaborador colaboradorSalvo() {
        ArgumentCaptor<Colaborador> captor = ArgumentCaptor.forClass(Colaborador.class);
        verify(colaboradores).save(captor.capture());
        return captor.getValue();
    }
}

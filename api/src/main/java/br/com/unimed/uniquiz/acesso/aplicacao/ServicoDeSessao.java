package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.acesso.persistencia.Sessao;
import br.com.unimed.uniquiz.acesso.persistencia.SessaoRepository;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDePin;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeSessao {

    private static final Duration DURACAO_SESSAO_ADMIN = Duration.ofHours(8);

    private final SessaoRepository sessoes;
    private final AdministradorRepository administradores;
    private final ServicoDeColaboradores colaboradores;
    private final ServicoDePin pins;
    private final Duration duracaoSessaoColaborador;
    private final Clock relogio;

    public ServicoDeSessao(
            SessaoRepository sessoes,
            AdministradorRepository administradores,
            ServicoDeColaboradores colaboradores,
            ServicoDePin pins,
            PropriedadesDoUniQuiz propriedades,
            Clock relogio) {
        this.sessoes = sessoes;
        this.administradores = administradores;
        this.colaboradores = colaboradores;
        this.pins = pins;
        this.duracaoSessaoColaborador = propriedades.sessao().duracao();
        this.relogio = relogio;
    }

    public Duration duracaoSessaoColaborador() {
        return duracaoSessaoColaborador;
    }

    public Duration duracaoSessaoAdmin() {
        return DURACAO_SESSAO_ADMIN;
    }

    @Transactional(noRollbackFor = ErroDeNegocio.class)
    public String entrarComoColaborador(Long colaboradorId, String pin) {
        pins.verificarOuCriar(colaboradorId, pin);
        String token = Tokens.novo();
        Instant agora = relogio.instant();
        sessoes.save(
                Sessao.deColaborador(Tokens.hash(token), colaboradorId, agora, agora.plus(duracaoSessaoColaborador)));
        return token;
    }

    @Transactional
    public String entrarComoAdministrador(Long administradorId) {
        String token = Tokens.novo();
        Instant agora = relogio.instant();
        sessoes.save(
                Sessao.deAdministrador(Tokens.hash(token), administradorId, agora, agora.plus(DURACAO_SESSAO_ADMIN)));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioLogado> buscar(String token) {
        return sessoes.findById(Tokens.hash(token))
                .filter(sessao -> !sessao.expirada(relogio.instant()))
                .flatMap(this::usuarioDa);
    }

    private Optional<UsuarioLogado> usuarioDa(Sessao sessao) {
        if (sessao.getColaboradorId() != null) {
            return colaboradores.estaAtivo(sessao.getColaboradorId())
                    ? Optional.of(UsuarioLogado.colaborador(sessao.getColaboradorId()))
                    : Optional.empty();
        }
        return administradores
                .findById(sessao.getAdministradorId())
                .filter(Administrador::isAtivo)
                .map(administrador -> UsuarioLogado.doPainel(
                        administrador.getId(),
                        administrador.getPerfil(),
                        administrador.escopo(),
                        administrador.isPrecisaTrocarSenha()));
    }

    @Transactional
    public void encerrar(String token) {
        sessoes.deleteById(Tokens.hash(token));
    }

    @Transactional
    public void encerrarTodasDoAdministrador(Long administradorId) {
        sessoes.apagarDoAdministrador(administradorId);
    }

    @Transactional
    public void encerrarOutrasDoAdministrador(Long administradorId, String tokenMantido) {
        sessoes.apagarDoAdministradorExceto(administradorId, Tokens.hash(tokenMantido));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void apagarExpiradas() {
        sessoes.apagarExpiradas(relogio.instant());
    }
}

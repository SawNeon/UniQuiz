package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.persistencia.Sessao;
import br.com.unimed.uniquiz.acesso.persistencia.SessaoRepository;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
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
    private final ServicoDeColaboradores colaboradores;
    private final Duration duracaoSessaoColaborador;
    private final Clock relogio;

    public ServicoDeSessao(
            SessaoRepository sessoes,
            ServicoDeColaboradores colaboradores,
            PropriedadesDoUniQuiz propriedades,
            Clock relogio) {
        this.sessoes = sessoes;
        this.colaboradores = colaboradores;
        this.duracaoSessaoColaborador = propriedades.sessao().duracao();
        this.relogio = relogio;
    }

    public Duration duracaoSessaoColaborador() {
        return duracaoSessaoColaborador;
    }

    public Duration duracaoSessaoAdmin() {
        return DURACAO_SESSAO_ADMIN;
    }

    @Transactional
    public String entrarComoColaborador(Long colaboradorId) {
        if (!colaboradores.estaAtivo(colaboradorId)) {
            throw new ErroDeNegocio("Não encontramos esse nome entre os colaboradores ativos.");
        }
        String token = Tokens.novo();
        Instant agora = relogio.instant();
        sessoes.save(Sessao.deColaborador(
                Tokens.hash(token), colaboradorId, agora, agora.plus(duracaoSessaoColaborador)));
        return token;
    }

    @Transactional
    public String entrarComoAdministrador(Long administradorId) {
        String token = Tokens.novo();
        Instant agora = relogio.instant();
        sessoes.save(Sessao.deAdministrador(Tokens.hash(token), administradorId, agora, agora.plus(DURACAO_SESSAO_ADMIN)));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioLogado> buscar(String token) {
        return sessoes.findById(Tokens.hash(token))
                .filter(sessao -> !sessao.expirada(relogio.instant()))
                .filter(sessao -> sessao.getColaboradorId() == null || colaboradores.estaAtivo(sessao.getColaboradorId()))
                .map(sessao -> sessao.getColaboradorId() != null
                        ? new UsuarioLogado(UsuarioLogado.Perfil.COLABORADOR, sessao.getColaboradorId())
                        : new UsuarioLogado(UsuarioLogado.Perfil.ADMINISTRADOR, sessao.getAdministradorId()));
    }

    @Transactional
    public void encerrar(String token) {
        sessoes.deleteById(Tokens.hash(token));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void apagarExpiradas() {
        sessoes.apagarExpiradas(relogio.instant());
    }
}

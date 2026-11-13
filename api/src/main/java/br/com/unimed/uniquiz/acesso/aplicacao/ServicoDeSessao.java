package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.DescricaoDoDispositivo;
import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.acesso.persistencia.Sessao;
import br.com.unimed.uniquiz.acesso.persistencia.Sessao.LimitesDaSessao;
import br.com.unimed.uniquiz.acesso.persistencia.SessaoRepository;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.AcaoAuditada;
import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDePin;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDePin.ResultadoDoPin;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.OrigemDaRequisicao;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeSessao {

    public static final LimitesDaSessao LIMITES_DO_PAINEL =
            new LimitesDaSessao(Duration.ofHours(8), Duration.ofMinutes(30));

    public record SessaoAtiva(
            String id, Instant criadaEm, Instant ultimoUsoEm, String ip, String dispositivo, boolean atual) {}

    private final SessaoRepository sessoes;
    private final AdministradorRepository administradores;
    private final ServicoDeColaboradores colaboradores;
    private final ServicoDePin pins;
    private final ServicoDeAuditoria auditoria;
    private final OrigemDaRequisicao origem;
    private final LimitesDaSessao limitesDoColaborador;
    private final Clock relogio;

    public ServicoDeSessao(
            SessaoRepository sessoes,
            AdministradorRepository administradores,
            ServicoDeColaboradores colaboradores,
            ServicoDePin pins,
            ServicoDeAuditoria auditoria,
            OrigemDaRequisicao origem,
            PropriedadesDoUniQuiz propriedades,
            Clock relogio) {
        this.sessoes = sessoes;
        this.administradores = administradores;
        this.colaboradores = colaboradores;
        this.pins = pins;
        this.auditoria = auditoria;
        this.origem = origem;
        this.limitesDoColaborador = new LimitesDaSessao(propriedades.sessao().duracao(), null);
        this.relogio = relogio;
    }

    public Duration duracaoSessaoColaborador() {
        return limitesDoColaborador.duracao();
    }

    public Duration duracaoSessaoAdmin() {
        return LIMITES_DO_PAINEL.duracao();
    }

    @Transactional(noRollbackFor = ErroDeNegocio.class)
    public String entrarComoColaborador(Long colaboradorId, String pin) {
        OcorrenciaDeAuditoria entrada =
                OcorrenciaDeAuditoria.de(TipoDeAtor.COLABORADOR, colaboradorId, AcaoAuditada.ENTRADA_DO_COLABORADOR);
        ResultadoDoPin resultado;
        try {
            resultado = pins.verificarOuCriar(colaboradorId, pin);
        } catch (ErroDeNegocio erro) {
            auditoria.registrar(entrada.comFalha(erro.getMessage()));
            throw erro;
        }
        String token = Tokens.novo();
        sessoes.save(Sessao.deColaborador(
                Tokens.hash(token), colaboradorId, relogio.instant(), limitesDoColaborador, origemAtual()));
        auditoria.registrar(
                resultado == ResultadoDoPin.PIN_CRIADO ? entrada.comDetalhe("Primeiro acesso: PIN criado") : entrada);
        return token;
    }

    @Transactional
    public String entrarComoAdministrador(Long administradorId) {
        String token = Tokens.novo();
        sessoes.save(Sessao.deAdministrador(
                Tokens.hash(token), administradorId, relogio.instant(), LIMITES_DO_PAINEL, origemAtual()));
        return token;
    }

    @Transactional
    public Optional<UsuarioLogado> buscar(String token) {
        Instant agora = relogio.instant();
        return sessoes.findById(Tokens.hash(token))
                .filter(sessao -> !sessao.expirada(agora))
                .flatMap(sessao -> {
                    sessao.registrarUso(agora);
                    return usuarioDa(sessao);
                });
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
        sessoes.findById(Tokens.hash(token)).ifPresent(sessao -> {
            sessoes.delete(sessao);
            auditoria.registrar(
                    sessao.getAdministradorId() != null
                            ? OcorrenciaDeAuditoria.de(
                                    TipoDeAtor.ADMINISTRADOR, sessao.getAdministradorId(), AcaoAuditada.SAIDA_DO_PAINEL)
                            : OcorrenciaDeAuditoria.de(
                                    TipoDeAtor.COLABORADOR,
                                    sessao.getColaboradorId(),
                                    AcaoAuditada.SAIDA_DO_COLABORADOR));
        });
    }

    @Transactional(readOnly = true)
    public List<SessaoAtiva> sessoesDoAdministrador(Long administradorId, String tokenAtual) {
        Instant agora = relogio.instant();
        String hashAtual = tokenAtual == null ? null : Tokens.hash(tokenAtual);
        return sessoes.findByAdministradorIdOrderByUltimoUsoEmDesc(administradorId).stream()
                .filter(sessao -> !sessao.expirada(agora))
                .map(sessao -> new SessaoAtiva(
                        sessao.identificadorPublico(),
                        sessao.getCriadaEm(),
                        sessao.getUltimoUsoEm(),
                        sessao.getIp(),
                        sessao.getDispositivo(),
                        sessao.getTokenHash().equals(hashAtual)))
                .toList();
    }

    @Transactional
    public void encerrarSessaoDoAdministrador(Long administradorId, String identificador) {
        Sessao sessao = sessoes.findByAdministradorIdOrderByUltimoUsoEmDesc(administradorId).stream()
                .filter(candidata -> candidata.identificadorPublico().equals(identificador))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontrado("Sessão não encontrada."));
        sessoes.delete(sessao);
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

    private Sessao.Origem origemAtual() {
        return new Sessao.Origem(
                origem.ipAtual().orElse(null),
                DescricaoDoDispositivo.de(origem.navegadorAtual().orElse(null)));
    }
}

package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.AcaoAuditada;
import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicoDeLoginAdmin {

    private static final String CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos.";
    private static final String ACESSO_BLOQUEADO =
            "Acesso bloqueado por excesso de tentativas. Tente de novo em 15 minutos.";

    private final AdministradorRepository administradores;
    private final ServicoDeSessao sessoes;
    private final ServicoDeAuditoria auditoria;
    private final PasswordEncoder codificador;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final String hashDeReferencia;

    public ServicoDeLoginAdmin(
            AdministradorRepository administradores,
            ServicoDeSessao sessoes,
            ServicoDeAuditoria auditoria,
            PasswordEncoder codificador,
            PlatformTransactionManager gerenciadorDeTransacoes,
            Clock relogio) {
        this.administradores = administradores;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
        this.codificador = codificador;
        this.transacao = new TransactionTemplate(gerenciadorDeTransacoes);
        this.relogio = relogio;
        this.hashDeReferencia = codificador.encode("referencia-para-tempo-constante");
    }

    public String entrar(String usuario, String senha) {
        Verificacao verificacao = transacao.execute(estado -> verificar(usuario, senha));
        auditoria.registrar(verificacao.ocorrencia());
        if (verificacao.recusa() != null) {
            throw new ErroDeNegocio(verificacao.recusa());
        }
        return sessoes.entrarComoAdministrador(verificacao.administradorId());
    }

    private Verificacao verificar(String usuario, String senha) {
        Instant agora = relogio.instant();
        Optional<Administrador> encontrado =
                administradores.buscarTravadoPorUsuario(usuario).filter(Administrador::isAtivo);
        if (encontrado.isEmpty()) {
            codificador.matches(senha, hashDeReferencia);
            return Verificacao.recusada(
                    OcorrenciaDeAuditoria.de(TipoDeAtor.ANONIMO, null, AcaoAuditada.ENTRADA_NO_PAINEL)
                            .descrevendoOAtor(usuario)
                            .comFalha("Usuário inexistente ou desativado"),
                    CREDENCIAIS_INVALIDAS);
        }
        Administrador administrador = encontrado.get();
        OcorrenciaDeAuditoria entrada = OcorrenciaDeAuditoria.de(
                TipoDeAtor.ADMINISTRADOR, administrador.getId(), AcaoAuditada.ENTRADA_NO_PAINEL);
        if (administrador.bloqueado(agora)) {
            return Verificacao.recusada(entrada.comFalha("Conta bloqueada"), ACESSO_BLOQUEADO);
        }
        if (!codificador.matches(senha, administrador.getSenhaHash())) {
            administrador.registrarFalha(agora);
            return Verificacao.recusada(
                    entrada.comFalha(
                            administrador.bloqueado(agora) ? "Senha incorreta; conta bloqueada" : "Senha incorreta"),
                    CREDENCIAIS_INVALIDAS);
        }
        administrador.registrarAcesso(agora);
        return new Verificacao(entrada, null, administrador.getId());
    }

    private record Verificacao(OcorrenciaDeAuditoria ocorrencia, String recusa, Long administradorId) {

        static Verificacao recusada(OcorrenciaDeAuditoria ocorrencia, String recusa) {
            return new Verificacao(ocorrencia, recusa, null);
        }
    }
}

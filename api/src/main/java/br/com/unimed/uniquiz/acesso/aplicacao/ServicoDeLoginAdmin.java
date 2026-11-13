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
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeLoginAdmin {

    private static final String CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos.";
    private static final String ACESSO_BLOQUEADO =
            "Acesso bloqueado por excesso de tentativas. Tente de novo em 15 minutos.";

    private final AdministradorRepository administradores;
    private final ServicoDeSessao sessoes;
    private final ServicoDeAuditoria auditoria;
    private final PasswordEncoder codificador;
    private final Clock relogio;
    private final String hashDeReferencia;

    public ServicoDeLoginAdmin(
            AdministradorRepository administradores,
            ServicoDeSessao sessoes,
            ServicoDeAuditoria auditoria,
            PasswordEncoder codificador,
            Clock relogio) {
        this.administradores = administradores;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
        this.codificador = codificador;
        this.relogio = relogio;
        this.hashDeReferencia = codificador.encode("referencia-para-tempo-constante");
    }

    @Transactional(noRollbackFor = ErroDeNegocio.class)
    public String entrar(String usuario, String senha) {
        Instant agora = relogio.instant();
        Optional<Administrador> encontrado =
                administradores.findByUsuario(usuario).filter(Administrador::isAtivo);
        if (encontrado.isEmpty()) {
            codificador.matches(senha, hashDeReferencia);
            auditoria.registrar(OcorrenciaDeAuditoria.de(TipoDeAtor.ANONIMO, null, AcaoAuditada.ENTRADA_NO_PAINEL)
                    .descrevendoOAtor(usuario)
                    .comFalha("Usuário inexistente ou desativado"));
            throw new ErroDeNegocio(CREDENCIAIS_INVALIDAS);
        }
        Administrador administrador = encontrado.get();
        OcorrenciaDeAuditoria entrada = OcorrenciaDeAuditoria.de(
                TipoDeAtor.ADMINISTRADOR, administrador.getId(), AcaoAuditada.ENTRADA_NO_PAINEL);
        if (administrador.bloqueado(agora)) {
            auditoria.registrar(entrada.comFalha("Conta bloqueada"));
            throw new ErroDeNegocio(ACESSO_BLOQUEADO);
        }
        if (!codificador.matches(senha, administrador.getSenhaHash())) {
            administrador.registrarFalha(agora);
            auditoria.registrar(entrada.comFalha(
                    administrador.bloqueado(agora) ? "Senha incorreta; conta bloqueada" : "Senha incorreta"));
            throw new ErroDeNegocio(CREDENCIAIS_INVALIDAS);
        }
        administrador.registrarAcesso(agora);
        auditoria.registrar(entrada);
        return sessoes.entrarComoAdministrador(administrador.getId());
    }
}

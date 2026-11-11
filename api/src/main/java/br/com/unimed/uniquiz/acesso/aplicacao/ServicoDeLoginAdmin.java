package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
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

    private final AdministradorRepository administradores;
    private final ServicoDeSessao sessoes;
    private final PasswordEncoder codificador;
    private final Clock relogio;
    private final String hashDeReferencia;

    public ServicoDeLoginAdmin(
            AdministradorRepository administradores,
            ServicoDeSessao sessoes,
            PasswordEncoder codificador,
            Clock relogio) {
        this.administradores = administradores;
        this.sessoes = sessoes;
        this.codificador = codificador;
        this.relogio = relogio;
        this.hashDeReferencia = codificador.encode("referencia-para-tempo-constante");
    }

    @Transactional(noRollbackFor = ErroDeNegocio.class)
    public String entrar(String usuario, String senha) {
        Instant agora = relogio.instant();
        Optional<Administrador> encontrado = administradores.findByUsuario(usuario).filter(Administrador::isAtivo);
        if (encontrado.isEmpty()) {
            codificador.matches(senha, hashDeReferencia);
            throw new ErroDeNegocio(CREDENCIAIS_INVALIDAS);
        }
        Administrador administrador = encontrado.get();
        if (administrador.bloqueado(agora)) {
            throw new ErroDeNegocio("Acesso bloqueado por excesso de tentativas. Tente de novo em 15 minutos.");
        }
        if (!codificador.matches(senha, administrador.getSenhaHash())) {
            administrador.registrarFalha(agora);
            throw new ErroDeNegocio(CREDENCIAIS_INVALIDAS);
        }
        administrador.registrarSucesso();
        return sessoes.entrarComoAdministrador(administrador.getId());
    }
}

package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import br.com.unimed.uniquiz.acesso.dominio.PoliticaDeSenha;
import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import java.time.Clock;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class InicializadorDeAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorDeAdministrador.class);

    private final AdministradorRepository administradores;
    private final PasswordEncoder codificador;
    private final PropriedadesDoUniQuiz.AdminInicial adminInicial;
    private final Clock relogio;

    InicializadorDeAdministrador(
            AdministradorRepository administradores,
            PasswordEncoder codificador,
            PropriedadesDoUniQuiz propriedades,
            Clock relogio) {
        this.administradores = administradores;
        this.codificador = codificador;
        this.adminInicial = propriedades.adminInicial();
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (administradores.count() > 0) {
            return;
        }
        String usuario = adminInicial.usuario();
        String senha = adminInicial.senha();
        if (usuario == null || usuario.isBlank() || senha == null || senha.length() < PoliticaDeSenha.TAMANHO_MINIMO) {
            log.warn(
                    "Nenhum administrador cadastrado. Defina UNIQUIZ_ADMIN_USUARIO e UNIQUIZ_ADMIN_SENHA "
                            + "(mínimo de {} caracteres) para criar o primeiro.",
                    PoliticaDeSenha.TAMANHO_MINIMO);
            return;
        }
        String usuarioLimpo = usuario.strip();
        administradores.save(new Administrador(
                usuarioLimpo,
                usuarioLimpo,
                codificador.encode(senha),
                PerfilDoPainel.ADMINISTRADOR,
                Set.of(),
                adminInicial.exigirTrocaDeSenha(),
                relogio.instant()));
        log.info("Administrador inicial '{}' criado.", usuarioLimpo);
    }
}

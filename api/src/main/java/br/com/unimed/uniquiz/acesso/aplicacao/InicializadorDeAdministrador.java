package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
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
    private static final int TAMANHO_MINIMO_DA_SENHA = 12;

    private final AdministradorRepository administradores;
    private final PasswordEncoder codificador;
    private final PropriedadesDoUniQuiz.AdminInicial adminInicial;

    InicializadorDeAdministrador(
            AdministradorRepository administradores, PasswordEncoder codificador, PropriedadesDoUniQuiz propriedades) {
        this.administradores = administradores;
        this.codificador = codificador;
        this.adminInicial = propriedades.adminInicial();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (administradores.count() > 0) {
            return;
        }
        String usuario = adminInicial.usuario();
        String senha = adminInicial.senha();
        if (usuario == null || usuario.isBlank() || senha == null || senha.length() < TAMANHO_MINIMO_DA_SENHA) {
            log.warn(
                    "Nenhum administrador cadastrado. Defina UNIQUIZ_ADMIN_USUARIO e UNIQUIZ_ADMIN_SENHA "
                            + "(mínimo de {} caracteres) para criar o primeiro.",
                    TAMANHO_MINIMO_DA_SENHA);
            return;
        }
        administradores.save(new Administrador(usuario.strip(), codificador.encode(senha)));
        log.info("Administrador inicial '{}' criado.", usuario.strip());
    }
}

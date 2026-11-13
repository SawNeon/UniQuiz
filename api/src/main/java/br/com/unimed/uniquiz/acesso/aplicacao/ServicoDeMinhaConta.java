package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.PoliticaDeSenha;
import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeMinhaConta {

    private final AdministradorRepository administradores;
    private final ServicoDeSessao sessoes;
    private final PasswordEncoder codificador;
    private final Clock relogio;

    public ServicoDeMinhaConta(
            AdministradorRepository administradores,
            ServicoDeSessao sessoes,
            PasswordEncoder codificador,
            Clock relogio) {
        this.administradores = administradores;
        this.sessoes = sessoes;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    @Transactional(noRollbackFor = ErroDeNegocio.class)
    public void trocarSenha(Long id, String senhaAtual, String novaSenha, String tokenDaSessaoAtual) {
        Administrador administrador = administradores
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontrado("Usuário do painel não encontrado."));
        Instant agora = relogio.instant();
        if (administrador.bloqueado(agora)) {
            throw new ErroDeNegocio("Acesso bloqueado por excesso de tentativas. Tente de novo em 15 minutos.");
        }
        if (!codificador.matches(senhaAtual, administrador.getSenhaHash())) {
            administrador.registrarFalha(agora);
            throw new ErroDeNegocio("A senha atual não confere.");
        }
        List<String> problemas = new ArrayList<>(PoliticaDeSenha.problemas(novaSenha, administrador.getUsuario()));
        if (novaSenha != null && novaSenha.equals(senhaAtual)) {
            problemas.add("A nova senha precisa ser diferente da atual.");
        }
        if (!problemas.isEmpty()) {
            throw new ErroDeNegocio(String.join(" ", problemas));
        }
        administrador.trocarSenha(codificador.encode(novaSenha), agora);
        sessoes.encerrarOutrasDoAdministrador(id, tokenDaSessaoAtual);
    }
}

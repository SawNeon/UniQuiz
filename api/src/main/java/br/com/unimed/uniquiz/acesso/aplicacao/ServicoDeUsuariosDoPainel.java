package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import br.com.unimed.uniquiz.acesso.dominio.Permissao;
import br.com.unimed.uniquiz.acesso.persistencia.Administrador;
import br.com.unimed.uniquiz.acesso.persistencia.AdministradorRepository;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeUsuariosDoPainel {

    public record UsuarioDoPainel(
            Long id,
            String nome,
            String usuario,
            PerfilDoPainel perfil,
            Set<Long> setores,
            boolean ativo,
            boolean bloqueado,
            boolean precisaTrocarSenha,
            Instant criadoEm,
            Instant ultimoAcessoEm) {}

    public record ContaLogada(
            Long id,
            String nome,
            String usuario,
            PerfilDoPainel perfil,
            Set<Permissao> permissoes,
            boolean precisaTrocarSenha) {}

    public record DadosDoUsuario(String nome, PerfilDoPainel perfil, Set<Long> setores) {}

    public record SenhaProvisoria(Long usuarioId, String usuario, String senha) {}

    private static final Pattern FORMATO_DO_USUARIO = Pattern.compile("[a-z0-9._-]{3,60}");

    private final AdministradorRepository administradores;
    private final ServicoDeSessao sessoes;
    private final ServicoDeColaboradores colaboradores;
    private final GeradorDeSenhaProvisoria gerador;
    private final PasswordEncoder codificador;
    private final Clock relogio;

    public ServicoDeUsuariosDoPainel(
            AdministradorRepository administradores,
            ServicoDeSessao sessoes,
            ServicoDeColaboradores colaboradores,
            GeradorDeSenhaProvisoria gerador,
            PasswordEncoder codificador,
            Clock relogio) {
        this.administradores = administradores;
        this.sessoes = sessoes;
        this.colaboradores = colaboradores;
        this.gerador = gerador;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDoPainel> listar() {
        Instant agora = relogio.instant();
        return administradores.findAllByOrderByNomeAsc().stream()
                .map(administrador -> paraLista(administrador, agora))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaLogada conta(Long id) {
        Administrador administrador = buscar(id);
        boolean trocaPendente = administrador.isPrecisaTrocarSenha();
        return new ContaLogada(
                administrador.getId(),
                administrador.getNome(),
                administrador.getUsuario(),
                administrador.getPerfil(),
                trocaPendente ? Set.of() : administrador.getPerfil().permissoes(),
                trocaPendente);
    }

    @Transactional
    public SenhaProvisoria criar(String usuario, DadosDoUsuario dados) {
        String usuarioNormalizado = normalizarUsuario(usuario);
        if (administradores.existsByUsuario(usuarioNormalizado)) {
            throw new ErroDeNegocio("Já existe um usuário do painel com esse login.");
        }
        exigirSetoresValidos(dados);
        String senha = gerador.gerar(usuarioNormalizado);
        Administrador novo = administradores.save(new Administrador(
                usuarioNormalizado,
                dados.nome().strip(),
                codificador.encode(senha),
                dados.perfil(),
                dados.setores(),
                true,
                relogio.instant()));
        return new SenhaProvisoria(novo.getId(), novo.getUsuario(), senha);
    }

    @Transactional
    public void editar(Long id, DadosDoUsuario dados, Long quemAltera) {
        Administrador administrador = buscar(id);
        exigirSetoresValidos(dados);
        if (dados.perfil() != PerfilDoPainel.ADMINISTRADOR) {
            exigirQueNaoTireOUltimoAdministrador(administrador, quemAltera, "tirar o próprio perfil de Administrador");
        }
        administrador.renomear(dados.nome());
        administrador.alterarPerfil(dados.perfil(), dados.setores());
    }

    @Transactional
    public void desativar(Long id, Long quemAltera) {
        Administrador administrador = buscar(id);
        exigirQueNaoTireOUltimoAdministrador(administrador, quemAltera, "desativar a própria conta");
        administrador.desativar();
        sessoes.encerrarTodasDoAdministrador(id);
    }

    @Transactional
    public void reativar(Long id) {
        buscar(id).reativar();
    }

    @Transactional
    public void desbloquear(Long id) {
        buscar(id).desbloquear();
    }

    @Transactional
    public SenhaProvisoria redefinirSenha(Long id, Long quemAltera) {
        if (id.equals(quemAltera)) {
            throw new ErroDeNegocio("Para trocar a sua própria senha, use Minha conta.");
        }
        Administrador administrador = buscar(id);
        String senha = gerador.gerar(administrador.getUsuario());
        administrador.definirSenhaProvisoria(codificador.encode(senha), relogio.instant());
        sessoes.encerrarTodasDoAdministrador(id);
        return new SenhaProvisoria(administrador.getId(), administrador.getUsuario(), senha);
    }

    @Transactional
    public void encerrarSessoes(Long id) {
        buscar(id);
        sessoes.encerrarTodasDoAdministrador(id);
    }

    private void exigirQueNaoTireOUltimoAdministrador(
            Administrador alvo, Long quemAltera, String descricaoDaAcaoSobreSiMesmo) {
        if (alvo.getId().equals(quemAltera)) {
            throw new ErroDeNegocio("Você não pode " + descricaoDaAcaoSobreSiMesmo + ".");
        }
        if (alvo.administradorAtivo() && administradores.countByPerfilAndAtivoTrue(PerfilDoPainel.ADMINISTRADOR) <= 1) {
            throw new ErroDeNegocio("É preciso manter ao menos um Administrador ativo.");
        }
    }

    private void exigirSetoresValidos(DadosDoUsuario dados) {
        if (!dados.perfil().limitadoAosSetores()) {
            return;
        }
        if (dados.setores().isEmpty()) {
            throw new ErroDeNegocio("Escolha ao menos um setor para o Gestor.");
        }
        if (!colaboradores.setoresExistem(dados.setores())) {
            throw new ErroDeNegocio("Um dos setores escolhidos não existe mais.");
        }
    }

    private static String normalizarUsuario(String usuario) {
        String normalizado = usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
        if (!FORMATO_DO_USUARIO.matcher(normalizado).matches()) {
            throw new ErroDeNegocio(
                    "O login precisa ter de 3 a 60 caracteres, só com letras sem acento, números, ponto, hífen ou sublinhado.");
        }
        return normalizado;
    }

    private Administrador buscar(Long id) {
        return administradores
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontrado("Usuário do painel não encontrado."));
    }

    private static UsuarioDoPainel paraLista(Administrador administrador, Instant agora) {
        return new UsuarioDoPainel(
                administrador.getId(),
                administrador.getNome(),
                administrador.getUsuario(),
                administrador.getPerfil(),
                administrador.getSetores(),
                administrador.isAtivo(),
                administrador.bloqueado(agora),
                administrador.isPrecisaTrocarSenha(),
                administrador.getCriadoEm(),
                administrador.getUltimoAcessoEm());
    }
}

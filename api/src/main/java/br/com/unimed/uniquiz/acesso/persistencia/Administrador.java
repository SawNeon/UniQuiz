package br.com.unimed.uniquiz.acesso.persistencia;

import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Administrador {

    public static final int TENTATIVAS_ANTES_DO_BLOQUEIO = 5;
    public static final Duration TEMPO_DE_BLOQUEIO = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario;
    private String nome;
    private String senhaHash;
    private boolean ativo;
    private int tentativasFalhas;
    private Instant bloqueadoAte;

    @Enumerated(EnumType.STRING)
    private PerfilDoPainel perfil;

    private boolean precisaTrocarSenha;
    private Instant senhaAlteradaEm;
    private Instant criadoEm;
    private Instant ultimoAcessoEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "administrador_setor", joinColumns = @JoinColumn(name = "administrador_id"))
    @Column(name = "setor_id")
    private Set<Long> setores = new HashSet<>();

    protected Administrador() {}

    public Administrador(
            String usuario,
            String nome,
            String senhaHash,
            PerfilDoPainel perfil,
            Set<Long> setores,
            boolean senhaProvisoria,
            Instant agora) {
        this.usuario = usuario;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.ativo = true;
        this.precisaTrocarSenha = senhaProvisoria;
        this.senhaAlteradaEm = agora;
        this.criadoEm = agora;
        alterarPerfil(perfil, setores);
    }

    public boolean bloqueado(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public void registrarFalha(Instant agora) {
        tentativasFalhas++;
        if (tentativasFalhas >= TENTATIVAS_ANTES_DO_BLOQUEIO) {
            bloqueadoAte = agora.plus(TEMPO_DE_BLOQUEIO);
            tentativasFalhas = 0;
        }
    }

    public void registrarAcesso(Instant agora) {
        desbloquear();
        ultimoAcessoEm = agora;
    }

    public void desbloquear() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    public void trocarSenha(String novoHash, Instant agora) {
        senhaHash = novoHash;
        senhaAlteradaEm = agora;
        precisaTrocarSenha = false;
    }

    public void definirSenhaProvisoria(String novoHash, Instant agora) {
        senhaHash = novoHash;
        senhaAlteradaEm = agora;
        precisaTrocarSenha = true;
        desbloquear();
    }

    public void renomear(String novoNome) {
        nome = novoNome.strip();
    }

    public void alterarPerfil(PerfilDoPainel novoPerfil, Set<Long> novosSetores) {
        perfil = novoPerfil;
        setores.clear();
        if (novoPerfil.limitadoAosSetores()) {
            setores.addAll(novosSetores);
        }
    }

    public void desativar() {
        ativo = false;
    }

    public void reativar() {
        ativo = true;
    }

    public boolean administradorAtivo() {
        return ativo && perfil == PerfilDoPainel.ADMINISTRADOR;
    }

    public EscopoDeSetores escopo() {
        return EscopoDeSetores.para(perfil, setores);
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNome() {
        return nome;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public PerfilDoPainel getPerfil() {
        return perfil;
    }

    public boolean isPrecisaTrocarSenha() {
        return precisaTrocarSenha;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getUltimoAcessoEm() {
        return ultimoAcessoEm;
    }

    public Set<Long> getSetores() {
        return Set.copyOf(setores);
    }
}

package br.com.unimed.uniquiz.colaborador.persistencia;

import br.com.unimed.uniquiz.comum.Textos;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.Instant;

@Entity
public class Colaborador {

    public static final int TENTATIVAS_DE_PIN_ANTES_DO_BLOQUEIO = 5;
    public static final Duration TEMPO_DE_BLOQUEIO_DO_PIN = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long empresaId;
    private Long setorId;
    private String nome;
    private String nomeNormalizado;
    private boolean ativo;
    private Instant criadoEm;
    private String pinHash;
    private int tentativasFalhasPin;
    private Instant pinBloqueadoAte;

    protected Colaborador() {}

    public Colaborador(Long empresaId, Long setorId, String nome, Instant criadoEm) {
        this.empresaId = empresaId;
        this.setorId = setorId;
        this.ativo = true;
        this.criadoEm = criadoEm;
        renomear(nome);
    }

    public void renomear(String novoNome) {
        this.nome = Textos.limpar(novoNome);
        this.nomeNormalizado = Textos.normalizar(novoNome);
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public boolean temPin() {
        return pinHash != null;
    }

    public void definirPin(String hash) {
        this.pinHash = hash;
        liberarPin();
    }

    public void removerPin() {
        this.pinHash = null;
        liberarPin();
    }

    public boolean pinBloqueado(Instant agora) {
        return pinBloqueadoAte != null && agora.isBefore(pinBloqueadoAte);
    }

    public void registrarFalhaDePin(Instant agora) {
        tentativasFalhasPin++;
        if (tentativasFalhasPin >= TENTATIVAS_DE_PIN_ANTES_DO_BLOQUEIO) {
            pinBloqueadoAte = agora.plus(TEMPO_DE_BLOQUEIO_DO_PIN);
            tentativasFalhasPin = 0;
        }
    }

    public void registrarAcertoDePin() {
        liberarPin();
    }

    public int tentativasDePinRestantes() {
        return TENTATIVAS_DE_PIN_ANTES_DO_BLOQUEIO - tentativasFalhasPin;
    }

    public Instant getPinBloqueadoAte() {
        return pinBloqueadoAte;
    }

    private void liberarPin() {
        tentativasFalhasPin = 0;
        pinBloqueadoAte = null;
    }

    public String getPinHash() {
        return pinHash;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public Long getSetorId() {
        return setorId;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeNormalizado() {
        return nomeNormalizado;
    }

    public boolean isAtivo() {
        return ativo;
    }
}

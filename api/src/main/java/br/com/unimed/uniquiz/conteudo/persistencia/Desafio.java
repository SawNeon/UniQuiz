package br.com.unimed.uniquiz.conteudo.persistencia;

import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import br.com.unimed.uniquiz.conteudo.dominio.TipoDesafio;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Desafio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long licaoId;

    @Enumerated(EnumType.STRING)
    private TipoDesafio tipo;

    private String enunciado;

    @Enumerated(EnumType.STRING)
    private Dificuldade dificuldade;

    private int pontos;
    private String explicacao;
    private String dica;
    private String conteudoJson;
    private int versao;
    private int ordem;

    @Enumerated(EnumType.STRING)
    private StatusPublicacao status;

    protected Desafio() {}

    public Desafio(Long licaoId, DadosDoDesafio dados, int ordem) {
        this.licaoId = licaoId;
        this.ordem = ordem;
        this.versao = 1;
        this.status = StatusPublicacao.RASCUNHO;
        aplicar(dados);
    }

    public record DadosDoDesafio(
            TipoDesafio tipo,
            String enunciado,
            Dificuldade dificuldade,
            int pontos,
            String explicacao,
            String dica,
            String conteudoJson) {}

    public void atualizar(DadosDoDesafio dados) {
        if (status == StatusPublicacao.PUBLICADO) {
            versao++;
        }
        aplicar(dados);
    }

    public Desafio copiar(int novaOrdem) {
        return new Desafio(licaoId, dados(), novaOrdem);
    }

    public DadosDoDesafio dados() {
        return new DadosDoDesafio(tipo, enunciado, dificuldade, pontos, explicacao, dica, conteudoJson);
    }

    public void mudarStatus(StatusPublicacao novoStatus) {
        this.status = novoStatus;
    }

    public void mover(int novaOrdem) {
        this.ordem = novaOrdem;
    }

    private void aplicar(DadosDoDesafio dados) {
        this.tipo = dados.tipo();
        this.enunciado = dados.enunciado().strip();
        this.dificuldade = dados.dificuldade();
        this.pontos = dados.pontos();
        this.explicacao = dados.explicacao().strip();
        this.dica = dados.dica() == null || dados.dica().isBlank() ? null : dados.dica().strip();
        this.conteudoJson = dados.conteudoJson();
    }

    public Long getId() {
        return id;
    }

    public Long getLicaoId() {
        return licaoId;
    }

    public TipoDesafio getTipo() {
        return tipo;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public Dificuldade getDificuldade() {
        return dificuldade;
    }

    public int getPontos() {
        return pontos;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public String getDica() {
        return dica;
    }

    public String getConteudoJson() {
        return conteudoJson;
    }

    public int getVersao() {
        return versao;
    }

    public int getOrdem() {
        return ordem;
    }

    public StatusPublicacao getStatus() {
        return status;
    }
}

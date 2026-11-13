package br.com.unimed.uniquiz.acesso.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.Instant;

@Entity
public class Sessao {

    public static final Duration INTERVALO_MINIMO_ENTRE_REGISTROS_DE_USO = Duration.ofMinutes(1);
    private static final int TAMANHO_DO_IDENTIFICADOR_PUBLICO = 16;

    @Id
    private String tokenHash;

    private Long colaboradorId;
    private Long administradorId;
    private Instant criadaEm;
    private Instant expiraEm;
    private Instant ultimoUsoEm;

    @Column(name = "inatividade_maxima_minutos")
    private Integer inatividadeMaximaMinutos;

    private String ip;
    private String dispositivo;

    protected Sessao() {}

    private Sessao(
            String tokenHash,
            Long colaboradorId,
            Long administradorId,
            Instant agora,
            Duration duracao,
            Duration inatividadeMaxima,
            String ip,
            String dispositivo) {
        this.tokenHash = tokenHash;
        this.colaboradorId = colaboradorId;
        this.administradorId = administradorId;
        this.criadaEm = agora;
        this.ultimoUsoEm = agora;
        this.expiraEm = agora.plus(duracao);
        this.inatividadeMaximaMinutos = inatividadeMaxima == null ? null : (int) inatividadeMaxima.toMinutes();
        this.ip = ip;
        this.dispositivo = dispositivo;
    }

    public static Sessao deColaborador(
            String tokenHash, Long colaboradorId, Instant agora, LimitesDaSessao limites, Origem origem) {
        return new Sessao(
                tokenHash,
                colaboradorId,
                null,
                agora,
                limites.duracao(),
                limites.inatividadeMaxima(),
                origem.ip(),
                origem.dispositivo());
    }

    public static Sessao deAdministrador(
            String tokenHash, Long administradorId, Instant agora, LimitesDaSessao limites, Origem origem) {
        return new Sessao(
                tokenHash,
                null,
                administradorId,
                agora,
                limites.duracao(),
                limites.inatividadeMaxima(),
                origem.ip(),
                origem.dispositivo());
    }

    public record LimitesDaSessao(Duration duracao, Duration inatividadeMaxima) {}

    public record Origem(String ip, String dispositivo) {}

    public boolean expirada(Instant agora) {
        return !agora.isBefore(expiraEm) || ociosaDemais(agora);
    }

    private boolean ociosaDemais(Instant agora) {
        return inatividadeMaximaMinutos != null
                && !agora.isBefore(ultimoUsoEm.plus(Duration.ofMinutes(inatividadeMaximaMinutos)));
    }

    public boolean registrarUso(Instant agora) {
        if (agora.isBefore(ultimoUsoEm.plus(INTERVALO_MINIMO_ENTRE_REGISTROS_DE_USO))) {
            return false;
        }
        ultimoUsoEm = agora;
        return true;
    }

    public String identificadorPublico() {
        return tokenHash.substring(0, TAMANHO_DO_IDENTIFICADOR_PUBLICO);
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public Long getAdministradorId() {
        return administradorId;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getUltimoUsoEm() {
        return ultimoUsoEm;
    }

    public String getIp() {
        return ip;
    }

    public String getDispositivo() {
        return dispositivo;
    }
}

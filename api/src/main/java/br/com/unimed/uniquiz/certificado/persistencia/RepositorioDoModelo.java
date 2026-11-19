package br.com.unimed.uniquiz.certificado.persistencia;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDoModelo {

    public record ModeloGuardado(String layout, byte[] fundoPdf, String nomeDaArte, Instant atualizadoEm) {}

    private final JdbcClient jdbc;

    public RepositorioDoModelo(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Instant> versao() {
        return jdbc.sql("SELECT atualizado_em FROM modelo_de_certificado WHERE id = 1")
                .query((linha, numero) -> instante(linha.getObject("atualizado_em", LocalDateTime.class)))
                .optional();
    }

    public Optional<ModeloGuardado> ler() {
        return jdbc.sql("SELECT layout, fundo_pdf, nome_da_arte, atualizado_em FROM modelo_de_certificado WHERE id = 1")
                .query((linha, numero) -> new ModeloGuardado(
                        linha.getString("layout"),
                        linha.getBytes("fundo_pdf"),
                        linha.getString("nome_da_arte"),
                        instante(linha.getObject("atualizado_em", LocalDateTime.class))))
                .optional();
    }

    public Optional<byte[]> previaDaArte() {
        return jdbc.sql("SELECT fundo_previa FROM modelo_de_certificado WHERE id = 1 AND fundo_previa IS NOT NULL")
                .query((linha, numero) -> linha.getBytes("fundo_previa"))
                .optional();
    }

    public void salvarLayout(String layout, Instant momento, Long administradorId) {
        int alterados = jdbc.sql(
                        """
                        UPDATE modelo_de_certificado
                        SET layout = :layout, atualizado_em = :momento, atualizado_por = :administrador
                        WHERE id = 1
                        """)
                .param("layout", layout)
                .param("momento", utc(momento))
                .param("administrador", administradorId, Types.BIGINT)
                .update();
        if (alterados == 0) {
            inserir(layout, null, null, null, momento, administradorId);
        }
    }

    public void salvarArte(
            String layoutSeNovo,
            byte[] fundoPdf,
            byte[] previa,
            String nomeDaArte,
            Instant momento,
            Long administradorId) {
        int alterados = jdbc.sql(
                        """
                        UPDATE modelo_de_certificado
                        SET fundo_pdf = :fundo, fundo_previa = :previa, nome_da_arte = :nome,
                            atualizado_em = :momento, atualizado_por = :administrador
                        WHERE id = 1
                        """)
                .param("fundo", fundoPdf, Types.VARBINARY)
                .param("previa", previa, Types.VARBINARY)
                .param("nome", nomeDaArte)
                .param("momento", utc(momento))
                .param("administrador", administradorId, Types.BIGINT)
                .update();
        if (alterados == 0) {
            inserir(layoutSeNovo, fundoPdf, previa, nomeDaArte, momento, administradorId);
        }
    }

    private void inserir(
            String layout, byte[] fundoPdf, byte[] previa, String nomeDaArte, Instant momento, Long administradorId) {
        jdbc.sql(
                        """
                        INSERT INTO modelo_de_certificado
                            (id, layout, fundo_pdf, fundo_previa, nome_da_arte, atualizado_em, atualizado_por)
                        VALUES (1, :layout, :fundo, :previa, :nome, :momento, :administrador)
                        """)
                .param("layout", layout)
                .param("fundo", fundoPdf, Types.VARBINARY)
                .param("previa", previa, Types.VARBINARY)
                .param("nome", nomeDaArte)
                .param("momento", utc(momento))
                .param("administrador", administradorId, Types.BIGINT)
                .update();
    }

    private static LocalDateTime utc(Instant momento) {
        return LocalDateTime.ofInstant(momento, ZoneOffset.UTC);
    }

    private static Instant instante(LocalDateTime momento) {
        return momento.toInstant(ZoneOffset.UTC);
    }
}

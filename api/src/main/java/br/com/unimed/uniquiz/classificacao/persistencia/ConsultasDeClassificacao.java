package br.com.unimed.uniquiz.classificacao.persistencia;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultasDeClassificacao {

    public record Linha(long colaboradorId, int posicao, String nome, String setor, long valor) {}

    private static final String POR_PONTOS =
            """
            WITH placar AS (
                SELECT e.colaborador_id, SUM(e.pontos) AS pontos, MAX(e.criado_em) AS atingiu_em
                FROM evento_pontos e
                WHERE e.temporada_id = :temporadaId
                GROUP BY e.colaborador_id
                HAVING SUM(e.pontos) > 0
            ),
            ranqueado AS (
                SELECT colaborador_id, pontos,
                       ROW_NUMBER() OVER (ORDER BY pontos DESC, atingiu_em ASC) AS posicao
                FROM placar
            ),
            minha AS (
                SELECT posicao FROM ranqueado WHERE colaborador_id = :colaboradorId
            )
            SELECT r.colaborador_id, r.posicao, c.nome, s.nome AS setor, r.pontos AS valor
            FROM ranqueado r
            JOIN colaborador c ON c.id = r.colaborador_id
            JOIN setor s ON s.id = c.setor_id
            WHERE r.posicao <= :limite
               OR r.colaborador_id = :colaboradorId
               OR r.posicao = (SELECT posicao - 1 FROM minha)
            ORDER BY r.posicao
            """;

    private static final String MAIS_RAPIDOS =
            """
            WITH melhores AS (
                SELECT t.colaborador_id, t.tempo_ms, t.concluida_em,
                       ROW_NUMBER() OVER (
                           PARTITION BY t.colaborador_id
                           ORDER BY t.tempo_ms ASC, t.concluida_em ASC) AS rn
                FROM tentativa t
                WHERE t.licao_id = :licaoId
                  AND t.temporada_id = :temporadaId
                  AND t.status = 'CONCLUIDA'
                  AND t.perfeita = 1
            ),
            ranqueado AS (
                SELECT colaborador_id, tempo_ms,
                       ROW_NUMBER() OVER (ORDER BY tempo_ms ASC, concluida_em ASC) AS posicao
                FROM melhores
                WHERE rn = 1
            )
            SELECT r.colaborador_id, r.posicao, c.nome, s.nome AS setor, r.tempo_ms AS valor
            FROM ranqueado r
            JOIN colaborador c ON c.id = r.colaborador_id
            JOIN setor s ON s.id = c.setor_id
            WHERE r.posicao <= :limite OR r.colaborador_id = :colaboradorId
            ORDER BY r.posicao
            """;

    private final JdbcClient jdbc;

    public ConsultasDeClassificacao(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Linha> porPontos(long temporadaId, long colaboradorId, int limite) {
        return jdbc.sql(POR_PONTOS)
                .param("temporadaId", temporadaId)
                .param("colaboradorId", colaboradorId)
                .param("limite", limite)
                .query(Linha.class)
                .list();
    }

    public List<Linha> maisRapidos(long temporadaId, long licaoId, long colaboradorId, int limite) {
        return jdbc.sql(MAIS_RAPIDOS)
                .param("temporadaId", temporadaId)
                .param("licaoId", licaoId)
                .param("colaboradorId", colaboradorId)
                .param("limite", limite)
                .query(Linha.class)
                .list();
    }
}

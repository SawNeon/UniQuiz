package br.com.unimed.uniquiz.relatorio.persistencia;

import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultasDeRelatorios {

    public record Setor(long id, String nome, String empresa) {}

    public record Pessoa(long id, String nome, long setorId, String setor, String empresa) {}

    public record LicaoComModulo(long licaoId, String licao, long moduloId, String modulo) {}

    public record Conclusao(long colaboradorId, long licaoId) {}

    public record UltimaAtividade(long colaboradorId, Instant momento) {}

    public record TentativasDaLicao(long licaoId, long iniciadas, long concluidas, long perfeitas, long abandonadas) {}

    public record RespostasDoDesafio(long desafioId, long respostas, long acertos) {}

    public record DesafioRespondido(
            long id, long licaoId, String enunciado, String tipo, String dificuldade, String conteudoJson) {}

    public record ErroRepetido(long desafioId, String respostaJson, long vezes) {}

    public record ConclusaoNoTempo(long colaboradorId, Instant momento) {}

    private final JdbcClient jdbc;

    public ConsultasDeRelatorios(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Setor> setores(EscopoDeSetores escopo) {
        Map<String, Object> parametros = new HashMap<>();
        return jdbc.sql("SELECT s.id, s.nome, e.nome AS empresa FROM setor s JOIN empresa e ON e.id = s.empresa_id"
                        + " WHERE " + escopo("s.id", escopo, parametros)
                        + " ORDER BY e.nome, s.nome")
                .params(parametros)
                .query(Setor.class)
                .list();
    }

    public List<Pessoa> pessoasAtivas(EscopoDeSetores escopo, Long setorId) {
        Map<String, Object> parametros = new HashMap<>();
        return jdbc.sql(
                        """
                        SELECT c.id, c.nome, c.setor_id, s.nome AS setor, e.nome AS empresa
                        FROM colaborador c
                        JOIN setor s ON s.id = c.setor_id
                        JOIN empresa e ON e.id = c.empresa_id
                        WHERE c.ativo = 1 AND %s
                        ORDER BY c.nome
                        """
                                .formatted(pessoasDoFiltro(escopo, setorId, parametros)))
                .params(parametros)
                .query(Pessoa.class)
                .list();
    }

    public List<LicaoComModulo> licoesPublicadas() {
        return jdbc.sql(
                        """
                        SELECT l.id AS licao_id, l.titulo AS licao, m.id AS modulo_id, m.titulo AS modulo
                        FROM licao l
                        JOIN modulo m ON m.id = l.modulo_id
                        WHERE l.status = 'PUBLICADO' AND m.status = 'PUBLICADO'
                        ORDER BY m.ordem, l.ordem
                        """)
                .query(LicaoComModulo.class)
                .list();
    }

    public List<Conclusao> conclusoes(EscopoDeSetores escopo, Long setorId) {
        Map<String, Object> parametros = new HashMap<>();
        return jdbc.sql(
                        """
                        SELECT DISTINCT t.colaborador_id, t.licao_id
                        FROM tentativa t
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE t.status = 'CONCLUIDA' AND c.ativo = 1 AND %s
                        """
                                .formatted(pessoasDoFiltro(escopo, setorId, parametros)))
                .params(parametros)
                .query(Conclusao.class)
                .list();
    }

    public List<UltimaAtividade> ultimasAtividades(EscopoDeSetores escopo, Long setorId) {
        Map<String, Object> parametros = new HashMap<>();
        return jdbc.sql(
                        """
                        SELECT t.colaborador_id, MAX(t.iniciada_em) AS momento
                        FROM tentativa t
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE c.ativo = 1 AND %s
                        GROUP BY t.colaborador_id
                        """
                                .formatted(pessoasDoFiltro(escopo, setorId, parametros)))
                .params(parametros)
                .query((resultado, numero) ->
                        new UltimaAtividade(resultado.getLong("colaborador_id"), instante(resultado, "momento")))
                .list();
    }

    private static String pessoasDoFiltro(EscopoDeSetores escopo, Long setorId, Map<String, Object> parametros) {
        String condicao = escopo("c.setor_id", escopo, parametros);
        if (setorId == null) {
            return condicao;
        }
        parametros.put("setorId", setorId);
        return condicao + " AND c.setor_id = :setorId";
    }

    public List<TentativasDaLicao> tentativasPorLicao(
            EscopoDeSetores escopo, Instant desde, Instant ate, Instant limiteDoAbandono) {
        Map<String, Object> parametros = periodo(desde, ate);
        parametros.put("limiteDoAbandono", utc(limiteDoAbandono));
        return jdbc.sql(
                        """
                        SELECT t.licao_id,
                               COUNT(*) AS iniciadas,
                               SUM(CASE WHEN t.status = 'CONCLUIDA' THEN 1 ELSE 0 END) AS concluidas,
                               SUM(CASE WHEN t.status = 'CONCLUIDA' AND t.perfeita = 1 THEN 1 ELSE 0 END) AS perfeitas,
                               SUM(CASE WHEN t.status = 'ABANDONADA'
                                         OR (t.status = 'EM_ANDAMENTO' AND t.iniciada_em < :limiteDoAbandono)
                                        THEN 1 ELSE 0 END) AS abandonadas
                        FROM tentativa t
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE t.iniciada_em >= :desde AND t.iniciada_em < :ate AND %s
                        GROUP BY t.licao_id
                        """
                                .formatted(escopo("c.setor_id", escopo, parametros)))
                .params(parametros)
                .query(TentativasDaLicao.class)
                .list();
    }

    public List<RespostasDoDesafio> primeirasRespostasPorDesafio(EscopoDeSetores escopo, Instant desde, Instant ate) {
        Map<String, Object> parametros = periodo(desde, ate);
        return jdbc.sql(
                        """
                        SELECT r.desafio_id,
                               COUNT(*) AS respostas,
                               SUM(CASE WHEN r.correta = 1 THEN 1 ELSE 0 END) AS acertos
                        FROM resposta r
                        JOIN tentativa t ON t.id = r.tentativa_id
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE r.segunda_chance = 0
                          AND r.respondida_em >= :desde AND r.respondida_em < :ate
                          AND %s
                        GROUP BY r.desafio_id
                        """
                                .formatted(escopo("c.setor_id", escopo, parametros)))
                .params(parametros)
                .query(RespostasDoDesafio.class)
                .list();
    }

    public List<ErroRepetido> errosPorResposta(EscopoDeSetores escopo, Instant desde, Instant ate) {
        Map<String, Object> parametros = periodo(desde, ate);
        return jdbc.sql(
                        """
                        SELECT r.desafio_id, CAST(r.resposta_json AS NVARCHAR(2000)) AS resposta_json, COUNT(*) AS vezes
                        FROM resposta r
                        JOIN tentativa t ON t.id = r.tentativa_id
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE r.segunda_chance = 0 AND r.correta = 0
                          AND r.respondida_em >= :desde AND r.respondida_em < :ate
                          AND %s
                        GROUP BY r.desafio_id, CAST(r.resposta_json AS NVARCHAR(2000))
                        """
                                .formatted(escopo("c.setor_id", escopo, parametros)))
                .params(parametros)
                .query(ErroRepetido.class)
                .list();
    }

    public List<LicaoComModulo> licoes(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(
                        """
                        SELECT l.id AS licao_id, l.titulo AS licao, m.id AS modulo_id, m.titulo AS modulo
                        FROM licao l
                        JOIN modulo m ON m.id = l.modulo_id
                        WHERE l.id IN (:ids)
                        ORDER BY m.ordem, l.ordem
                        """)
                .param("ids", ids)
                .query(LicaoComModulo.class)
                .list();
    }

    public List<DesafioRespondido> desafios(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(
                        """
                        SELECT id, licao_id, enunciado, tipo, dificuldade, conteudo_json
                        FROM desafio
                        WHERE id IN (:ids)
                        """)
                .param("ids", ids)
                .query(DesafioRespondido.class)
                .list();
    }

    public List<ConclusaoNoTempo> conclusoesDesde(EscopoDeSetores escopo, Instant desde) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("desde", utc(desde));
        return jdbc.sql(
                        """
                        SELECT t.colaborador_id, t.concluida_em AS momento
                        FROM tentativa t
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE t.status = 'CONCLUIDA' AND t.concluida_em >= :desde AND %s
                        """
                                .formatted(escopo("c.setor_id", escopo, parametros)))
                .params(parametros)
                .query(ConsultasDeRelatorios::conclusaoNoTempo)
                .list();
    }

    public List<ConclusaoNoTempo> primeirasConclusoes(EscopoDeSetores escopo) {
        Map<String, Object> parametros = new HashMap<>();
        return jdbc.sql(
                        """
                        SELECT t.colaborador_id, MIN(t.concluida_em) AS momento
                        FROM tentativa t
                        JOIN colaborador c ON c.id = t.colaborador_id
                        WHERE t.status = 'CONCLUIDA' AND %s
                        GROUP BY t.colaborador_id
                        """
                                .formatted(escopo("c.setor_id", escopo, parametros)))
                .params(parametros)
                .query(ConsultasDeRelatorios::conclusaoNoTempo)
                .list();
    }

    public long pessoasAtivasNoEscopo(EscopoDeSetores escopo) {
        Map<String, Object> parametros = new HashMap<>();
        return jdbc.sql("SELECT COUNT(*) FROM colaborador c WHERE c.ativo = 1 AND "
                        + escopo("c.setor_id", escopo, parametros))
                .params(parametros)
                .query(Long.class)
                .single();
    }

    private static String escopo(String coluna, EscopoDeSetores escopo, Map<String, Object> parametros) {
        if (escopo.irrestrito()) {
            return "1 = 1";
        }
        if (escopo.setores().isEmpty()) {
            return "1 = 0";
        }
        parametros.put("setoresDoEscopo", escopo.setores());
        return coluna + " IN (:setoresDoEscopo)";
    }

    private static Map<String, Object> periodo(Instant desde, Instant ate) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("desde", utc(desde));
        parametros.put("ate", utc(ate));
        return parametros;
    }

    private static ConclusaoNoTempo conclusaoNoTempo(ResultSet resultado, int numero) throws SQLException {
        return new ConclusaoNoTempo(resultado.getLong("colaborador_id"), instante(resultado, "momento"));
    }

    private static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static Instant instante(ResultSet resultado, String coluna) throws SQLException {
        LocalDateTime valor = resultado.getObject(coluna, LocalDateTime.class);
        return valor == null ? null : valor.toInstant(ZoneOffset.UTC);
    }
}

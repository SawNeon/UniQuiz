package br.com.unimed.uniquiz.auditoria.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultasDeAuditoria {

    public record Filtro(String atorTipo, String acao, String resultado, String busca, Instant desde, Instant ate) {}

    public record Linha(
            long id,
            Instant momento,
            String atorTipo,
            Long atorId,
            String atorNome,
            String acao,
            String alvo,
            String resultado,
            String detalhe,
            String ip) {}

    public record FalhasDeEntrada(String quem, String ip, long falhas, Instant ultima) {}

    public record ContaBloqueada(String tipo, long id, String nome, Instant ate) {}

    private static final String NOME_DO_ATOR =
            """
            CASE r.ator_tipo
                WHEN 'ADMINISTRADOR' THEN a.nome
                WHEN 'COLABORADOR' THEN c.nome
                ELSE r.ator_descricao
            END
            """;

    private static final String ORIGEM =
            """
            FROM registro_de_auditoria r
            LEFT JOIN administrador a ON r.ator_tipo = 'ADMINISTRADOR' AND a.id = r.ator_id
            LEFT JOIN colaborador c ON r.ator_tipo = 'COLABORADOR' AND c.id = r.ator_id
            """;

    private static final String FALHAS_POR_PESSOA =
            """
            SELECT %s AS quem, NULL AS ip, COUNT(*) AS falhas, MAX(r.momento) AS ultima
            %s
            WHERE r.resultado = 'FALHA'
              AND r.acao IN ('ENTRADA_NO_PAINEL', 'ENTRADA_DO_COLABORADOR')
              AND r.momento >= :desde
            GROUP BY r.ator_tipo, r.ator_id, %s
            HAVING COUNT(*) >= :minimo
            ORDER BY falhas DESC, ultima DESC
            """
                    .formatted(NOME_DO_ATOR, ORIGEM, NOME_DO_ATOR);

    private static final String FALHAS_POR_IP =
            """
            SELECT NULL AS quem, r.ip, COUNT(*) AS falhas, MAX(r.momento) AS ultima
            FROM registro_de_auditoria r
            WHERE r.resultado = 'FALHA'
              AND r.acao IN ('ENTRADA_NO_PAINEL', 'ENTRADA_DO_COLABORADOR')
              AND r.momento >= :desde
              AND r.ip IS NOT NULL
            GROUP BY r.ip
            HAVING COUNT(*) >= :minimo
            ORDER BY falhas DESC, ultima DESC
            """;

    private static final String CONTAS_BLOQUEADAS =
            """
            SELECT 'ADMINISTRADOR' AS tipo, id, nome, bloqueado_ate AS ate
            FROM administrador WHERE bloqueado_ate > :agora
            UNION ALL
            SELECT 'COLABORADOR' AS tipo, id, nome, pin_bloqueado_ate AS ate
            FROM colaborador WHERE pin_bloqueado_ate > :agora
            ORDER BY ate DESC
            """;

    private final JdbcClient jdbc;

    public ConsultasDeAuditoria(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Linha> buscar(Filtro filtro, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(filtro, parametros);
        parametros.put("inicio", pagina * tamanho);
        parametros.put("tamanho", tamanho);
        return jdbc.sql("SELECT r.*, " + NOME_DO_ATOR + " AS ator_nome " + ORIGEM + onde
                        + " ORDER BY r.momento DESC, r.id DESC OFFSET :inicio ROWS FETCH NEXT :tamanho ROWS ONLY")
                .params(parametros)
                .query(ConsultasDeAuditoria::linha)
                .list();
    }

    public long contar(Filtro filtro) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(filtro, parametros);
        return jdbc.sql("SELECT COUNT(*) " + ORIGEM + onde)
                .params(parametros)
                .query(Long.class)
                .single();
    }

    public List<FalhasDeEntrada> falhasPorPessoa(Instant desde, int minimo) {
        return falhas(FALHAS_POR_PESSOA, desde, minimo);
    }

    public List<FalhasDeEntrada> falhasPorIp(Instant desde, int minimo) {
        return falhas(FALHAS_POR_IP, desde, minimo);
    }

    public List<ContaBloqueada> contasBloqueadas(Instant agora) {
        return jdbc.sql(CONTAS_BLOQUEADAS)
                .param("agora", utc(agora))
                .query((resultado, numero) -> new ContaBloqueada(
                        resultado.getString("tipo"),
                        resultado.getLong("id"),
                        resultado.getString("nome"),
                        instante(resultado, "ate")))
                .list();
    }

    private List<FalhasDeEntrada> falhas(String sql, Instant desde, int minimo) {
        return jdbc.sql(sql)
                .param("desde", utc(desde))
                .param("minimo", minimo)
                .query((resultado, numero) -> new FalhasDeEntrada(
                        resultado.getString("quem"),
                        resultado.getString("ip"),
                        resultado.getLong("falhas"),
                        instante(resultado, "ultima")))
                .list();
    }

    private static String condicoes(Filtro filtro, Map<String, Object> parametros) {
        List<String> condicoes = new ArrayList<>();
        if (filtro.atorTipo() != null) {
            condicoes.add("r.ator_tipo = :atorTipo");
            parametros.put("atorTipo", filtro.atorTipo());
        }
        if (filtro.acao() != null) {
            condicoes.add("r.acao = :acao");
            parametros.put("acao", filtro.acao());
        }
        if (filtro.resultado() != null) {
            condicoes.add("r.resultado = :resultado");
            parametros.put("resultado", filtro.resultado());
        }
        if (filtro.busca() != null) {
            condicoes.add("(" + NOME_DO_ATOR + " LIKE :busca OR r.alvo LIKE :busca OR r.ip LIKE :busca)");
            parametros.put("busca", "%" + escaparCuringas(filtro.busca()) + "%");
        }
        if (filtro.desde() != null) {
            condicoes.add("r.momento >= :desde");
            parametros.put("desde", utc(filtro.desde()));
        }
        if (filtro.ate() != null) {
            condicoes.add("r.momento < :ate");
            parametros.put("ate", utc(filtro.ate()));
        }
        return condicoes.isEmpty() ? "" : " WHERE " + String.join(" AND ", condicoes);
    }

    private static String escaparCuringas(String texto) {
        return texto.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    private static Linha linha(ResultSet resultado, int numero) throws SQLException {
        Long atorId = resultado.getObject("ator_id", Long.class);
        return new Linha(
                resultado.getLong("id"),
                instante(resultado, "momento"),
                resultado.getString("ator_tipo"),
                atorId,
                resultado.getString("ator_nome"),
                resultado.getString("acao"),
                resultado.getString("alvo"),
                resultado.getString("resultado"),
                resultado.getString("detalhe"),
                resultado.getString("ip"));
    }

    private static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static Instant instante(ResultSet resultado, String coluna) throws SQLException {
        LocalDateTime valor = resultado.getObject(coluna, LocalDateTime.class);
        return valor == null ? null : valor.toInstant(ZoneOffset.UTC);
    }
}

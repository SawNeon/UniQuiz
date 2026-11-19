package br.com.unimed.uniquiz.certificado.persistencia;

import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
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
public class ConsultasDeCertificados {

    public record Filtro(Long moduloId, Long setorId, String busca) {}

    public record Emitido(
            String codigo,
            long moduloId,
            String modulo,
            long colaboradorId,
            String nome,
            String empresa,
            String setor,
            Instant emitidoEm) {}

    public record EmitidosPorModulo(long moduloId, long emitidos) {}

    private static final String ORIGEM =
            """
             FROM certificado ce
             JOIN colaborador c ON c.id = ce.colaborador_id
             JOIN setor s ON s.id = c.setor_id
             JOIN empresa e ON e.id = c.empresa_id
            """;

    private static final String COLUNAS =
            "SELECT ce.codigo, ce.modulo_id, ce.titulo_modulo, ce.colaborador_id, ce.nome_colaborador,"
                    + " e.nome AS empresa, s.nome AS setor, ce.emitido_em";

    private final JdbcClient jdbc;

    public ConsultasDeCertificados(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Emitido> buscar(EscopoDeSetores escopo, Filtro filtro, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(escopo, filtro, parametros);
        parametros.put("inicio", pagina * tamanho);
        parametros.put("tamanho", tamanho);
        return jdbc.sql(COLUNAS + ORIGEM + onde
                        + " ORDER BY ce.emitido_em DESC, ce.id DESC OFFSET :inicio ROWS FETCH NEXT :tamanho ROWS ONLY")
                .params(parametros)
                .query(ConsultasDeCertificados::emitido)
                .list();
    }

    public long contar(EscopoDeSetores escopo, Filtro filtro) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(escopo, filtro, parametros);
        return jdbc.sql("SELECT COUNT(*)" + ORIGEM + onde)
                .params(parametros)
                .query(Long.class)
                .single();
    }

    public List<Emitido> todos(EscopoDeSetores escopo, Filtro filtro) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(escopo, filtro, parametros);
        return jdbc.sql(COLUNAS + ORIGEM + onde + " ORDER BY ce.titulo_modulo, ce.nome_colaborador, ce.id")
                .params(parametros)
                .query(ConsultasDeCertificados::emitido)
                .list();
    }

    public List<EmitidosPorModulo> porModulo(EscopoDeSetores escopo) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(escopo, new Filtro(null, null, null), parametros);
        return jdbc.sql("SELECT ce.modulo_id, COUNT(*) AS emitidos" + ORIGEM + onde + " GROUP BY ce.modulo_id")
                .params(parametros)
                .query((linha, numero) -> new EmitidosPorModulo(linha.getLong("modulo_id"), linha.getLong("emitidos")))
                .list();
    }

    private static String condicoes(EscopoDeSetores escopo, Filtro filtro, Map<String, Object> parametros) {
        List<String> condicoes = new ArrayList<>();
        if (!escopo.irrestrito()) {
            if (escopo.setores().isEmpty()) {
                condicoes.add("1 = 0");
            } else {
                condicoes.add("c.setor_id IN (:setoresDoEscopo)");
                parametros.put("setoresDoEscopo", escopo.setores());
            }
        }
        if (filtro.moduloId() != null) {
            condicoes.add("ce.modulo_id = :moduloId");
            parametros.put("moduloId", filtro.moduloId());
        }
        if (filtro.setorId() != null) {
            condicoes.add("c.setor_id = :setorId");
            parametros.put("setorId", filtro.setorId());
        }
        if (filtro.busca() != null) {
            condicoes.add("(c.nome_normalizado LIKE :busca OR ce.codigo LIKE :busca)");
            parametros.put("busca", "%" + escaparCuringas(filtro.busca()) + "%");
        }
        return condicoes.isEmpty() ? "" : " WHERE " + String.join(" AND ", condicoes);
    }

    private static String escaparCuringas(String texto) {
        return texto.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    private static Emitido emitido(ResultSet linha, int numero) throws SQLException {
        return new Emitido(
                linha.getString("codigo"),
                linha.getLong("modulo_id"),
                linha.getString("titulo_modulo"),
                linha.getLong("colaborador_id"),
                linha.getString("nome_colaborador"),
                linha.getString("empresa"),
                linha.getString("setor"),
                linha.getObject("emitido_em", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }
}

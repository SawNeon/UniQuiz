package br.com.unimed.uniquiz.auditoria.aplicacao;

import br.com.unimed.uniquiz.auditoria.dominio.AcaoAuditada;
import br.com.unimed.uniquiz.auditoria.dominio.ResultadoDaAcao;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import br.com.unimed.uniquiz.auditoria.persistencia.ConsultasDeAuditoria;
import br.com.unimed.uniquiz.auditoria.persistencia.ConsultasDeAuditoria.ContaBloqueada;
import br.com.unimed.uniquiz.auditoria.persistencia.ConsultasDeAuditoria.FalhasDeEntrada;
import br.com.unimed.uniquiz.auditoria.persistencia.ConsultasDeAuditoria.Filtro;
import br.com.unimed.uniquiz.auditoria.persistencia.ConsultasDeAuditoria.Linha;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeConsultaDeAuditoria {

    public static final int TAMANHO_DA_PAGINA = 50;
    private static final Duration JANELA_DOS_ALERTAS = Duration.ofHours(24);
    private static final int FALHAS_PARA_ALERTAR_POR_PESSOA = 3;
    private static final int FALHAS_PARA_ALERTAR_POR_IP = 5;

    public record Pedido(
            TipoDeAtor atorTipo,
            AcaoAuditada acao,
            ResultadoDaAcao resultado,
            String busca,
            LocalDate de,
            LocalDate ate,
            int pagina) {}

    public record Registro(
            long id,
            Instant momento,
            TipoDeAtor atorTipo,
            Long atorId,
            String atorNome,
            AcaoAuditada acao,
            String acaoDescricao,
            String alvo,
            ResultadoDaAcao resultado,
            String detalhe,
            String ip) {}

    public record Pagina(List<Registro> itens, int pagina, int tamanho, long total) {}

    public record Alertas(
            Instant desde,
            List<FalhasDeEntrada> falhasPorPessoa,
            List<FalhasDeEntrada> falhasPorIp,
            List<ContaBloqueada> contasBloqueadas) {}

    public record OpcaoDeAcao(AcaoAuditada codigo, String descricao) {}

    private final ConsultasDeAuditoria consultas;
    private final Clock relogio;

    public ServicoDeConsultaDeAuditoria(ConsultasDeAuditoria consultas, Clock relogio) {
        this.consultas = consultas;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Pagina buscar(Pedido pedido) {
        if (pedido.de() != null && pedido.ate() != null && pedido.ate().isBefore(pedido.de())) {
            throw new ErroDeNegocio("A data final precisa ser igual ou posterior à inicial.");
        }
        int pagina = Math.max(pedido.pagina(), 0);
        Filtro filtro = new Filtro(
                nomeDe(pedido.atorTipo()),
                nomeDe(pedido.acao()),
                nomeDe(pedido.resultado()),
                pedido.busca() == null || pedido.busca().isBlank()
                        ? null
                        : pedido.busca().strip(),
                pedido.de() == null ? null : inicioDoDia(pedido.de()),
                pedido.ate() == null ? null : inicioDoDia(pedido.ate().plusDays(1)));
        List<Registro> itens = consultas.buscar(filtro, pagina, TAMANHO_DA_PAGINA).stream()
                .map(ServicoDeConsultaDeAuditoria::registro)
                .toList();
        return new Pagina(itens, pagina, TAMANHO_DA_PAGINA, consultas.contar(filtro));
    }

    @Transactional(readOnly = true)
    public Alertas alertas() {
        Instant agora = relogio.instant();
        Instant desde = agora.minus(JANELA_DOS_ALERTAS);
        return new Alertas(
                desde,
                consultas.falhasPorPessoa(desde, FALHAS_PARA_ALERTAR_POR_PESSOA),
                consultas.falhasPorIp(desde, FALHAS_PARA_ALERTAR_POR_IP),
                consultas.contasBloqueadas(agora));
    }

    public List<OpcaoDeAcao> acoes() {
        return Arrays.stream(AcaoAuditada.values())
                .map(acao -> new OpcaoDeAcao(acao, acao.descricao()))
                .toList();
    }

    private Instant inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(relogio.getZone()).toInstant();
    }

    private static String nomeDe(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    private static Registro registro(Linha linha) {
        AcaoAuditada acao = acaoConhecida(linha.acao());
        return new Registro(
                linha.id(),
                linha.momento(),
                TipoDeAtor.valueOf(linha.atorTipo()),
                linha.atorId(),
                linha.atorNome(),
                acao,
                acao == AcaoAuditada.ACAO_NAO_CATALOGADA ? linha.acao() : acao.descricao(),
                linha.alvo(),
                ResultadoDaAcao.valueOf(linha.resultado()),
                linha.detalhe(),
                linha.ip());
    }

    private static AcaoAuditada acaoConhecida(String codigo) {
        return Arrays.stream(AcaoAuditada.values())
                .filter(acao -> acao.name().equals(codigo))
                .findFirst()
                .orElse(AcaoAuditada.ACAO_NAO_CATALOGADA);
    }
}

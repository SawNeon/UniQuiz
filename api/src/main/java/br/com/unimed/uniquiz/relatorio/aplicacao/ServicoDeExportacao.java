package br.com.unimed.uniquiz.relatorio.aplicacao;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.AcaoAuditada;
import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.Desempenho;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.PedidoDePessoas;
import br.com.unimed.uniquiz.relatorio.dominio.CalibracaoDaDificuldade.Avaliacao;
import br.com.unimed.uniquiz.relatorio.dominio.EscritaEmCsv;
import br.com.unimed.uniquiz.relatorio.dominio.FormatoDeExportacao;
import br.com.unimed.uniquiz.relatorio.dominio.Planilha;
import br.com.unimed.uniquiz.relatorio.dominio.RelatorioExportavel;
import br.com.unimed.uniquiz.relatorio.dominio.SituacaoNoModulo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ServicoDeExportacao {

    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<Avaliacao, String> AVALIACOES = Map.of(
            Avaliacao.ADEQUADA, "Adequada",
            Avaliacao.MAIS_FACIL_QUE_O_PREVISTO, "Mais fácil que o previsto",
            Avaliacao.MAIS_DIFICIL_QUE_O_PREVISTO, "Mais difícil que o previsto",
            Avaliacao.POUCOS_DADOS, "Poucos dados");

    public record Parametros(
            Long setorId,
            Long moduloId,
            SituacaoNoModulo situacao,
            String busca,
            LocalDate de,
            LocalDate ate,
            Integer semanas) {}

    public record ArquivoExportado(String nome, String tipoDeConteudo, byte[] conteudo) {}

    private final ServicoDeRelatorios relatorios;
    private final ServicoDeAuditoria auditoria;
    private final Clock relogio;

    public ServicoDeExportacao(ServicoDeRelatorios relatorios, ServicoDeAuditoria auditoria, Clock relogio) {
        this.relatorios = relatorios;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public ArquivoExportado exportar(
            UsuarioLogado usuario, RelatorioExportavel relatorio, FormatoDeExportacao formato, Parametros parametros) {
        Planilha planilha =
                switch (relatorio) {
                    case CONFORMIDADE_PESSOAS -> conformidadePorPessoa(usuario, parametros);
                    case DESEMPENHO_LICOES -> desempenhoPorLicao(usuario, parametros);
                    case DESEMPENHO_DESAFIOS -> desempenhoPorDesafio(usuario, parametros);
                    case ENGAJAMENTO -> engajamento(usuario, parametros);
                };
        byte[] conteudo =
                formato == FormatoDeExportacao.CSV ? EscritaEmCsv.escrever(planilha) : EscritaEmXlsx.escrever(planilha);
        auditoria.registrar(
                OcorrenciaDeAuditoria.de(TipoDeAtor.ADMINISTRADOR, usuario.id(), AcaoAuditada.RELATORIO_EXPORTADO)
                        .comAlvo("relatorio=%s, formato=%s".formatted(relatorio.caminho(), formato.extensao()))
                        .comDetalhe(detalhe(planilha, parametros)));
        String nome =
                "uniquiz-%s-%s.%s".formatted(relatorio.nomeDoArquivo(), LocalDate.now(relogio), formato.extensao());
        return new ArquivoExportado(nome, formato.tipoDeConteudo(), conteudo);
    }

    private Planilha conformidadePorPessoa(UsuarioLogado usuario, Parametros parametros) {
        List<List<Object>> linhas = relatorios
                .todasAsPessoas(
                        usuario.escopo(),
                        new PedidoDePessoas(
                                parametros.setorId(), parametros.moduloId(), parametros.situacao(), parametros.busca()))
                .stream()
                .map(pessoa -> linha(
                        pessoa.nome(),
                        pessoa.empresa(),
                        pessoa.setor(),
                        pessoa.situacao().rotulo(),
                        pessoa.licoesConcluidas(),
                        pessoa.totalDeLicoes(),
                        pessoa.modulosConcluidos(),
                        pessoa.totalDeModulos(),
                        momento(pessoa.ultimaAtividade())))
                .toList();
        return new Planilha(
                "Conformidade",
                List.of(
                        "Nome",
                        "Empresa",
                        "Setor",
                        "Situação",
                        "Lições concluídas",
                        "Total de lições",
                        "Módulos concluídos",
                        "Total de módulos",
                        "Última atividade"),
                linhas);
    }

    private Planilha desempenhoPorLicao(UsuarioLogado usuario, Parametros parametros) {
        Desempenho desempenho = desempenho(usuario, parametros);
        List<List<Object>> linhas = desempenho.licoes().stream()
                .map(licao -> linha(
                        licao.modulo(),
                        licao.licao(),
                        licao.iniciadas(),
                        licao.concluidas(),
                        licao.perfeitas(),
                        licao.abandonadas(),
                        licao.abandono(),
                        licao.primeirasRespostas(),
                        licao.acertoNaPrimeira()))
                .toList();
        return new Planilha(
                "Desempenho por lição",
                List.of(
                        "Módulo",
                        "Lição",
                        "Tentativas iniciadas",
                        "Concluídas",
                        "Perfeitas",
                        "Abandonadas",
                        "Abandono (%)",
                        "Primeiras respostas",
                        "Acerto na 1ª resposta (%)"),
                linhas);
    }

    private Planilha desempenhoPorDesafio(UsuarioLogado usuario, Parametros parametros) {
        Desempenho desempenho = desempenho(usuario, parametros);
        List<List<Object>> linhas = desempenho.desafios().stream()
                .map(desafio -> linha(
                        desafio.modulo(),
                        desafio.licao(),
                        desafio.enunciado(),
                        desafio.tipo().name(),
                        desafio.dificuldade().name(),
                        desafio.respostas(),
                        desafio.acerto(),
                        desafio.dificuldadeObservada() == null
                                ? null
                                : desafio.dificuldadeObservada().name(),
                        AVALIACOES.get(desafio.calibracao()),
                        desafio.erroMaisComum(),
                        desafio.vezesDoErroMaisComum()))
                .toList();
        return new Planilha(
                "Desempenho por desafio",
                List.of(
                        "Módulo",
                        "Lição",
                        "Enunciado",
                        "Tipo",
                        "Dificuldade declarada",
                        "Primeiras respostas",
                        "Acerto (%)",
                        "Dificuldade observada",
                        "Calibração",
                        "Erro mais comum",
                        "Vezes do erro"),
                linhas);
    }

    private Planilha engajamento(UsuarioLogado usuario, Parametros parametros) {
        List<List<Object>> linhas = relatorios.engajamento(usuario.escopo(), parametros.semanas()).semanas().stream()
                .map(semana -> linha(
                        semana.inicio(),
                        semana.pessoasAtivas(),
                        semana.participacao(),
                        semana.licoesConcluidas(),
                        semana.novosParticipantes()))
                .toList();
        return new Planilha(
                "Engajamento",
                List.of(
                        "Semana que começa em",
                        "Pessoas ativas",
                        "Participação (%)",
                        "Lições concluídas",
                        "Novos participantes"),
                linhas);
    }

    private Desempenho desempenho(UsuarioLogado usuario, Parametros parametros) {
        return relatorios.desempenho(usuario.escopo(), parametros.de(), parametros.ate(), parametros.moduloId());
    }

    private String momento(Instant instante) {
        return instante == null ? null : MOMENTO.format(instante.atZone(relogio.getZone()));
    }

    private static List<Object> linha(Object... valores) {
        return new ArrayList<>(Arrays.asList(valores));
    }

    private static String detalhe(Planilha planilha, Parametros parametros) {
        List<String> partes = new ArrayList<>();
        partes.add(planilha.linhas().size() + " linhas");
        if (parametros.setorId() != null) {
            partes.add("setor=" + parametros.setorId());
        }
        if (parametros.moduloId() != null) {
            partes.add("modulo=" + parametros.moduloId());
        }
        if (parametros.situacao() != null) {
            partes.add("situacao=" + parametros.situacao().name());
        }
        if (parametros.de() != null || parametros.ate() != null) {
            partes.add(
                    "periodo=" + Objects.toString(parametros.de(), "") + ".." + Objects.toString(parametros.ate(), ""));
        }
        if (parametros.semanas() != null) {
            partes.add("semanas=" + parametros.semanas());
        }
        return partes.stream().collect(Collectors.joining("; "));
    }
}

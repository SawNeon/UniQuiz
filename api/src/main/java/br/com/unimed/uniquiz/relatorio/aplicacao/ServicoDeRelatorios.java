package br.com.unimed.uniquiz.relatorio.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.comum.Textos;
import br.com.unimed.uniquiz.conteudo.aplicacao.LeitorDeConteudo;
import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.TipoDesafio;
import br.com.unimed.uniquiz.relatorio.dominio.CalibracaoDaDificuldade;
import br.com.unimed.uniquiz.relatorio.dominio.CalibracaoDaDificuldade.Avaliacao;
import br.com.unimed.uniquiz.relatorio.dominio.DescricaoDaResposta;
import br.com.unimed.uniquiz.relatorio.dominio.Semanas;
import br.com.unimed.uniquiz.relatorio.dominio.SituacaoNoModulo;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.Conclusao;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.ConclusaoNoTempo;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.DesafioRespondido;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.ErroRepetido;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.LicaoComModulo;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.Pessoa;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.RespostasDoDesafio;
import br.com.unimed.uniquiz.relatorio.persistencia.ConsultasDeRelatorios.TentativasDaLicao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeRelatorios {

    public static final int TAMANHO_DA_PAGINA = 50;
    public static final int SEMANAS_PADRAO = 12;
    public static final int SEMANAS_NO_MAXIMO = 52;
    private static final int DIAS_PADRAO_DO_DESEMPENHO = 30;
    private static final int DIAS_NO_MAXIMO = 366;
    private static final Duration TEMPO_PARA_CONSIDERAR_ABANDONO = Duration.ofDays(1);

    public record OpcaoDeSetor(long id, String nome, String empresa) {}

    public record OpcaoDeModulo(long id, String titulo) {}

    public record Filtros(List<OpcaoDeSetor> setores, List<OpcaoDeModulo> modulos) {}

    public record ResumoDaTrilha(long concluiram, long emAndamento, long naoComecaram) {}

    public record ModuloNaConformidade(
            long id, String titulo, int licoes, long concluiram, long emAndamento, long naoComecaram) {}

    public record SetorNaConformidade(
            long id, String setor, String empresa, long pessoas, long concluiramATrilha, double licoesConcluidas) {}

    public record Conformidade(
            long pessoas,
            int totalDeLicoes,
            ResumoDaTrilha trilha,
            List<ModuloNaConformidade> modulos,
            List<SetorNaConformidade> setores) {}

    public record PessoaNaConformidade(
            long id,
            String nome,
            String setor,
            String empresa,
            int licoesConcluidas,
            int totalDeLicoes,
            int modulosConcluidos,
            int totalDeModulos,
            SituacaoNoModulo situacao,
            Instant ultimaAtividade) {}

    public record PedidoDePessoas(Long setorId, Long moduloId, SituacaoNoModulo situacao, String busca) {}

    public record PaginaDePessoas(List<PessoaNaConformidade> itens, int pagina, int tamanho, long total) {}

    public record LicaoNoDesempenho(
            long id,
            String modulo,
            String licao,
            long iniciadas,
            long concluidas,
            long perfeitas,
            long abandonadas,
            long primeirasRespostas,
            Double acertoNaPrimeira,
            Double abandono) {}

    public record DesafioNoDesempenho(
            long id,
            String modulo,
            String licao,
            String enunciado,
            TipoDesafio tipo,
            Dificuldade dificuldade,
            long respostas,
            long acertos,
            Double acerto,
            Dificuldade dificuldadeObservada,
            Avaliacao calibracao,
            String erroMaisComum,
            long vezesDoErroMaisComum) {}

    public record Desempenho(
            LocalDate de, LocalDate ate, List<LicaoNoDesempenho> licoes, List<DesafioNoDesempenho> desafios) {}

    public record SemanaDeEngajamento(
            LocalDate inicio,
            long pessoasAtivas,
            long licoesConcluidas,
            long novosParticipantes,
            double participacao) {}

    public record Engajamento(long pessoasNoEscopo, List<SemanaDeEngajamento> semanas) {}

    private record Modulo(long id, String titulo, List<Long> licoes) {}

    private final ConsultasDeRelatorios consultas;
    private final LeitorDeConteudo leitor;
    private final ObjectMapper json;
    private final Clock relogio;

    public ServicoDeRelatorios(
            ConsultasDeRelatorios consultas, LeitorDeConteudo leitor, ObjectMapper json, Clock relogio) {
        this.consultas = consultas;
        this.leitor = leitor;
        this.json = json;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Filtros filtros(EscopoDeSetores escopo) {
        return new Filtros(
                consultas.setores(escopo).stream()
                        .map(setor -> new OpcaoDeSetor(setor.id(), setor.nome(), setor.empresa()))
                        .toList(),
                modulosPublicados().stream()
                        .map(modulo -> new OpcaoDeModulo(modulo.id(), modulo.titulo()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public Conformidade conformidade(EscopoDeSetores escopo, Long setorId) {
        exigirSetorNoEscopo(escopo, setorId);
        List<Modulo> modulos = modulosPublicados();
        List<Pessoa> pessoas = consultas.pessoasAtivas(escopo, setorId);
        Map<Long, Set<Long>> concluidas = concluidasPorPessoa(consultas.conclusoes(escopo, setorId));
        int totalDeLicoes =
                modulos.stream().mapToInt(modulo -> modulo.licoes().size()).sum();

        List<ModuloNaConformidade> porModulo = modulos.stream()
                .map(modulo -> {
                    Map<SituacaoNoModulo, Long> contagem = pessoas.stream()
                            .collect(Collectors.groupingBy(
                                    pessoa -> situacaoNoModulo(modulo, concluidas.get(pessoa.id())),
                                    Collectors.counting()));
                    return new ModuloNaConformidade(
                            modulo.id(),
                            modulo.titulo(),
                            modulo.licoes().size(),
                            contagem.getOrDefault(SituacaoNoModulo.CONCLUIU, 0L),
                            contagem.getOrDefault(SituacaoNoModulo.EM_ANDAMENTO, 0L),
                            contagem.getOrDefault(SituacaoNoModulo.NAO_COMECOU, 0L));
                })
                .toList();

        Map<SituacaoNoModulo, Long> naTrilha = pessoas.stream()
                .collect(Collectors.groupingBy(
                        pessoa -> SituacaoNoModulo.de(
                                licoesConcluidas(modulos, concluidas.get(pessoa.id())), totalDeLicoes),
                        Collectors.counting()));

        Map<Long, List<Pessoa>> pessoasPorSetor = pessoas.stream()
                .collect(Collectors.groupingBy(Pessoa::setorId, LinkedHashMap::new, Collectors.toList()));
        List<SetorNaConformidade> porSetor = pessoasPorSetor.values().stream()
                .map(doSetor -> {
                    Pessoa primeira = doSetor.getFirst();
                    long concluiram = doSetor.stream()
                            .filter(pessoa -> totalDeLicoes > 0
                                    && licoesConcluidas(modulos, concluidas.get(pessoa.id())) >= totalDeLicoes)
                            .count();
                    double media = doSetor.stream()
                            .mapToDouble(pessoa ->
                                    percentual(licoesConcluidas(modulos, concluidas.get(pessoa.id())), totalDeLicoes))
                            .average()
                            .orElse(0);
                    return new SetorNaConformidade(
                            primeira.setorId(),
                            primeira.setor(),
                            primeira.empresa(),
                            doSetor.size(),
                            concluiram,
                            arredondar(media));
                })
                .sorted(Comparator.comparing(SetorNaConformidade::empresa).thenComparing(SetorNaConformidade::setor))
                .toList();

        return new Conformidade(
                pessoas.size(),
                totalDeLicoes,
                new ResumoDaTrilha(
                        naTrilha.getOrDefault(SituacaoNoModulo.CONCLUIU, 0L),
                        naTrilha.getOrDefault(SituacaoNoModulo.EM_ANDAMENTO, 0L),
                        naTrilha.getOrDefault(SituacaoNoModulo.NAO_COMECOU, 0L)),
                porModulo,
                porSetor);
    }

    @Transactional(readOnly = true)
    public PaginaDePessoas pessoas(EscopoDeSetores escopo, PedidoDePessoas pedido, int pagina) {
        List<PessoaNaConformidade> todas = todasAsPessoas(escopo, pedido);
        int inicio = Math.max(pagina, 0) * TAMANHO_DA_PAGINA;
        List<PessoaNaConformidade> itens = inicio >= todas.size()
                ? List.of()
                : todas.subList(inicio, Math.min(inicio + TAMANHO_DA_PAGINA, todas.size()));
        return new PaginaDePessoas(itens, Math.max(pagina, 0), TAMANHO_DA_PAGINA, todas.size());
    }

    @Transactional(readOnly = true)
    public List<PessoaNaConformidade> todasAsPessoas(EscopoDeSetores escopo, PedidoDePessoas pedido) {
        exigirSetorNoEscopo(escopo, pedido.setorId());
        List<Modulo> modulos = modulosPublicados();
        Optional<Modulo> moduloDoFiltro = Optional.ofNullable(pedido.moduloId()).map(id -> modulos.stream()
                .filter(modulo -> modulo.id() == id)
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontrado("Módulo não encontrado.")));
        Map<Long, Set<Long>> concluidas = concluidasPorPessoa(consultas.conclusoes(escopo, pedido.setorId()));
        Map<Long, Instant> atividades = consultas.ultimasAtividades(escopo, pedido.setorId()).stream()
                .collect(Collectors.toMap(
                        ConsultasDeRelatorios.UltimaAtividade::colaboradorId,
                        ConsultasDeRelatorios.UltimaAtividade::momento));
        int totalDeLicoes =
                modulos.stream().mapToInt(modulo -> modulo.licoes().size()).sum();
        String busca = pedido.busca() == null || pedido.busca().isBlank() ? null : Textos.normalizar(pedido.busca());

        return consultas.pessoasAtivas(escopo, pedido.setorId()).stream()
                .filter(pessoa ->
                        busca == null || Textos.normalizar(pessoa.nome()).contains(busca))
                .map(pessoa -> {
                    Set<Long> daPessoa = concluidas.get(pessoa.id());
                    int licoesConcluidas = licoesConcluidas(modulos, daPessoa);
                    int modulosConcluidos = (int) modulos.stream()
                            .filter(modulo -> situacaoNoModulo(modulo, daPessoa) == SituacaoNoModulo.CONCLUIU)
                            .count();
                    SituacaoNoModulo situacao = moduloDoFiltro
                            .map(modulo -> situacaoNoModulo(modulo, daPessoa))
                            .orElseGet(() -> SituacaoNoModulo.de(licoesConcluidas, totalDeLicoes));
                    return new PessoaNaConformidade(
                            pessoa.id(),
                            pessoa.nome(),
                            pessoa.setor(),
                            pessoa.empresa(),
                            licoesConcluidas,
                            totalDeLicoes,
                            modulosConcluidos,
                            modulos.size(),
                            situacao,
                            atividades.get(pessoa.id()));
                })
                .filter(pessoa -> pedido.situacao() == null || pessoa.situacao() == pedido.situacao())
                .toList();
    }

    @Transactional(readOnly = true)
    public Desempenho desempenho(EscopoDeSetores escopo, LocalDate de, LocalDate ate, Long moduloId) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate fim = ate == null ? hoje : ate;
        LocalDate inicio = de == null ? fim.minusDays(DIAS_PADRAO_DO_DESEMPENHO - 1L) : de;
        if (fim.isBefore(inicio)) {
            throw new ErroDeNegocio("A data final precisa ser igual ou posterior à inicial.");
        }
        if (inicio.plusDays(DIAS_NO_MAXIMO).isBefore(fim)) {
            throw new ErroDeNegocio("Escolha um período de até um ano.");
        }
        Instant desde = inicioDoDia(inicio);
        Instant antesDe = inicioDoDia(fim.plusDays(1));

        List<TentativasDaLicao> tentativas = consultas.tentativasPorLicao(
                escopo, desde, antesDe, relogio.instant().minus(TEMPO_PARA_CONSIDERAR_ABANDONO));
        List<RespostasDoDesafio> respostas = consultas.primeirasRespostasPorDesafio(escopo, desde, antesDe);
        Map<Long, DesafioRespondido> desafios =
                consultas
                        .desafios(respostas.stream()
                                .map(RespostasDoDesafio::desafioId)
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(DesafioRespondido::id, Function.identity()));

        Set<Long> licoesEnvolvidas = new HashSet<>();
        tentativas.forEach(linha -> licoesEnvolvidas.add(linha.licaoId()));
        desafios.values().forEach(desafio -> licoesEnvolvidas.add(desafio.licaoId()));
        Map<Long, LicaoComModulo> licoes = consultas.licoes(licoesEnvolvidas).stream()
                .collect(Collectors.toMap(
                        LicaoComModulo::licaoId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> licoesDoFiltro = licoes.values().stream()
                .filter(licao -> moduloId == null || licao.moduloId() == moduloId)
                .map(LicaoComModulo::licaoId)
                .collect(Collectors.toSet());

        Map<Long, long[]> primeirasPorLicao = new HashMap<>();
        for (RespostasDoDesafio linha : respostas) {
            DesafioRespondido desafio = desafios.get(linha.desafioId());
            long[] soma = primeirasPorLicao.computeIfAbsent(desafio.licaoId(), ignorado -> new long[2]);
            soma[0] += linha.respostas();
            soma[1] += linha.acertos();
        }
        Map<Long, TentativasDaLicao> tentativasPorLicao =
                tentativas.stream().collect(Collectors.toMap(TentativasDaLicao::licaoId, Function.identity()));

        List<LicaoNoDesempenho> porLicao = licoes.values().stream()
                .filter(licao -> licoesDoFiltro.contains(licao.licaoId()))
                .map(licao -> {
                    TentativasDaLicao contagem = tentativasPorLicao.getOrDefault(
                            licao.licaoId(), new TentativasDaLicao(licao.licaoId(), 0, 0, 0, 0));
                    long[] primeiras = primeirasPorLicao.getOrDefault(licao.licaoId(), new long[2]);
                    return new LicaoNoDesempenho(
                            licao.licaoId(),
                            licao.modulo(),
                            licao.licao(),
                            contagem.iniciadas(),
                            contagem.concluidas(),
                            contagem.perfeitas(),
                            contagem.abandonadas(),
                            primeiras[0],
                            taxa(primeiras[1], primeiras[0]),
                            taxa(contagem.abandonadas(), contagem.iniciadas()));
                })
                .toList();

        Map<Long, ErroRepetido> errosMaisComuns = consultas.errosPorResposta(escopo, desde, antesDe).stream()
                .collect(Collectors.toMap(
                        ErroRepetido::desafioId,
                        Function.identity(),
                        (um, outro) -> um.vezes() >= outro.vezes() ? um : outro));

        List<DesafioNoDesempenho> porDesafio = respostas.stream()
                .map(linha -> desafioNoDesempenho(
                        linha, desafios.get(linha.desafioId()), licoes, errosMaisComuns.get(linha.desafioId())))
                .filter(desafio ->
                        licoesDoFiltro.contains(desafios.get(desafio.id()).licaoId()))
                .sorted(Comparator.comparing(
                                (DesafioNoDesempenho desafio) -> desafio.acerto() == null ? 1.0 : desafio.acerto())
                        .thenComparing(DesafioNoDesempenho::id))
                .toList();

        return new Desempenho(inicio, fim, porLicao, porDesafio);
    }

    @Transactional(readOnly = true)
    public Engajamento engajamento(EscopoDeSetores escopo, Integer semanas) {
        int quantidade = semanas == null ? SEMANAS_PADRAO : semanas;
        if (quantidade < 1 || quantidade > SEMANAS_NO_MAXIMO) {
            throw new ErroDeNegocio("Escolha de 1 a %d semanas.".formatted(SEMANAS_NO_MAXIMO));
        }
        List<LocalDate> inicios = Semanas.ultimas(LocalDate.now(relogio), quantidade);
        Instant desde = inicioDoDia(inicios.getFirst());
        long pessoasNoEscopo = consultas.pessoasAtivasNoEscopo(escopo);

        Map<LocalDate, List<ConclusaoNoTempo>> porSemana = consultas.conclusoesDesde(escopo, desde).stream()
                .collect(Collectors.groupingBy(conclusao -> semanaDe(conclusao.momento())));
        Map<LocalDate, Long> novosPorSemana = consultas.primeirasConclusoes(escopo).stream()
                .filter(primeira -> !primeira.momento().isBefore(desde))
                .collect(Collectors.groupingBy(primeira -> semanaDe(primeira.momento()), Collectors.counting()));

        List<SemanaDeEngajamento> resultado = new ArrayList<>();
        for (LocalDate inicio : inicios) {
            List<ConclusaoNoTempo> daSemana = porSemana.getOrDefault(inicio, List.of());
            long ativas = daSemana.stream()
                    .map(ConclusaoNoTempo::colaboradorId)
                    .distinct()
                    .count();
            resultado.add(new SemanaDeEngajamento(
                    inicio,
                    ativas,
                    daSemana.size(),
                    novosPorSemana.getOrDefault(inicio, 0L),
                    arredondar(percentual(ativas, pessoasNoEscopo))));
        }
        return new Engajamento(pessoasNoEscopo, resultado);
    }

    private DesafioNoDesempenho desafioNoDesempenho(
            RespostasDoDesafio linha,
            DesafioRespondido desafio,
            Map<Long, LicaoComModulo> licoes,
            ErroRepetido erroMaisComum) {
        TipoDesafio tipo = TipoDesafio.valueOf(desafio.tipo());
        Dificuldade dificuldade = Dificuldade.valueOf(desafio.dificuldade());
        CalibracaoDaDificuldade calibracao =
                CalibracaoDaDificuldade.calibrar(dificuldade, linha.respostas(), linha.acertos());
        LicaoComModulo licao = licoes.get(desafio.licaoId());
        return new DesafioNoDesempenho(
                desafio.id(),
                licao == null ? null : licao.modulo(),
                licao == null ? null : licao.licao(),
                desafio.enunciado(),
                tipo,
                dificuldade,
                linha.respostas(),
                linha.acertos(),
                taxa(linha.acertos(), linha.respostas()),
                calibracao.observada(),
                calibracao.avaliacao(),
                erroMaisComum == null ? null : descrever(tipo, desafio.conteudoJson(), erroMaisComum.respostaJson()),
                erroMaisComum == null ? 0 : erroMaisComum.vezes());
    }

    private String descrever(TipoDesafio tipo, String conteudoJson, String respostaJson) {
        try {
            return DescricaoDaResposta.descrever(
                    leitor.ler(tipo, conteudoJson), leitor.lerResposta(tipo, json.readTree(respostaJson)));
        } catch (JsonProcessingException | ErroDeNegocio erro) {
            return "Resposta em formato antigo";
        }
    }

    private List<Modulo> modulosPublicados() {
        Map<Long, Modulo> modulos = new LinkedHashMap<>();
        for (LicaoComModulo licao : consultas.licoesPublicadas()) {
            modulos.computeIfAbsent(licao.moduloId(), id -> new Modulo(id, licao.modulo(), new ArrayList<>()))
                    .licoes()
                    .add(licao.licaoId());
        }
        return List.copyOf(modulos.values());
    }

    private static Map<Long, Set<Long>> concluidasPorPessoa(List<Conclusao> conclusoes) {
        return conclusoes.stream()
                .collect(Collectors.groupingBy(
                        Conclusao::colaboradorId, Collectors.mapping(Conclusao::licaoId, Collectors.toSet())));
    }

    private static SituacaoNoModulo situacaoNoModulo(Modulo modulo, Set<Long> concluidas) {
        long feitas = concluidas == null
                ? 0
                : modulo.licoes().stream().filter(concluidas::contains).count();
        return SituacaoNoModulo.de(feitas, modulo.licoes().size());
    }

    private static int licoesConcluidas(List<Modulo> modulos, Set<Long> concluidas) {
        if (concluidas == null) {
            return 0;
        }
        return (int) modulos.stream()
                .flatMap(modulo -> modulo.licoes().stream())
                .filter(concluidas::contains)
                .count();
    }

    private static void exigirSetorNoEscopo(EscopoDeSetores escopo, Long setorId) {
        if (setorId != null && !escopo.permite(setorId)) {
            throw new RecursoNaoEncontrado("Setor não encontrado.");
        }
    }

    private LocalDate semanaDe(Instant momento) {
        return Semanas.inicioDaSemana(LocalDate.ofInstant(momento, relogio.getZone()));
    }

    private Instant inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(relogio.getZone()).toInstant();
    }

    private static Double taxa(long parte, long total) {
        return total == 0 ? null : arredondar(percentual(parte, total));
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0 : 100.0 * parte / total;
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}

package br.com.unimed.uniquiz.jogo.aplicacao;

import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDePontos;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeTemporadas;
import br.com.unimed.uniquiz.classificacao.dominio.Ganho;
import br.com.unimed.uniquiz.classificacao.dominio.MotivoDosPontos;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoPublicado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoPublicado.DesafioEmJogo;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoPublicado.DesafioPublico;
import br.com.unimed.uniquiz.conteudo.aplicacao.LeitorDeConteudo;
import br.com.unimed.uniquiz.conteudo.aplicacao.ServicoDeConteudoPublicado;
import br.com.unimed.uniquiz.conteudo.dominio.Correcao;
import br.com.unimed.uniquiz.conteudo.dominio.RespostaDoColaborador;
import br.com.unimed.uniquiz.jogo.dominio.RegrasDePontuacao;
import br.com.unimed.uniquiz.jogo.dominio.RegrasDePontuacao.PontosDoAcerto;
import br.com.unimed.uniquiz.jogo.dominio.SequenciaDeDias;
import br.com.unimed.uniquiz.jogo.dominio.StatusDaTentativa;
import br.com.unimed.uniquiz.jogo.persistencia.Resposta;
import br.com.unimed.uniquiz.jogo.persistencia.RespostaRepository;
import br.com.unimed.uniquiz.jogo.persistencia.SequenciaDiaria;
import br.com.unimed.uniquiz.jogo.persistencia.SequenciaDiariaRepository;
import br.com.unimed.uniquiz.jogo.persistencia.Tentativa;
import br.com.unimed.uniquiz.jogo.persistencia.TentativaRepository;
import br.com.unimed.uniquiz.trofeu.aplicacao.ServicoDeTrofeus;
import br.com.unimed.uniquiz.trofeu.aplicacao.ServicoDeTrofeus.TrofeuDoColaborador;
import br.com.unimed.uniquiz.trofeu.dominio.RegrasDeTrofeus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeLicao {

    public record TentativaIniciada(
            Long tentativaId, String titulo, boolean valePontosIntegrais, List<DesafioPublico> desafios) {}

    public record RespostaCorrigida(
            boolean correta,
            String motivoDoErro,
            String respostaCerta,
            String explicacao,
            int pontos,
            int bonusDeSequencia,
            boolean segundaChance,
            List<Long> desafiosParaSegundaChance) {}

    public record ModuloConcluido(Long id, String titulo) {}

    public record ResultadoDaLicao(
            int acertos,
            int total,
            boolean perfeita,
            long tempoMs,
            List<Ganho> ganhos,
            int pontosGanhos,
            int diasSeguidos,
            List<TrofeuDoColaborador> trofeusNovos,
            ModuloConcluido moduloConcluido) {}

    private final ServicoDeConteudoPublicado conteudo;
    private final ServicoDeProgresso progresso;
    private final ServicoDeTemporadas temporadas;
    private final ServicoDePontos pontos;
    private final ServicoDeTrofeus trofeus;
    private final TentativaRepository tentativas;
    private final RespostaRepository respostas;
    private final SequenciaDiariaRepository sequencias;
    private final LeitorDeConteudo leitor;
    private final RegrasDePontuacao regras;
    private final RandomGenerator aleatorio;
    private final Clock relogio;

    public ServicoDeLicao(
            ServicoDeConteudoPublicado conteudo,
            ServicoDeProgresso progresso,
            ServicoDeTemporadas temporadas,
            ServicoDePontos pontos,
            ServicoDeTrofeus trofeus,
            TentativaRepository tentativas,
            RespostaRepository respostas,
            SequenciaDiariaRepository sequencias,
            LeitorDeConteudo leitor,
            RegrasDePontuacao regras,
            RandomGenerator aleatorio,
            Clock relogio) {
        this.conteudo = conteudo;
        this.progresso = progresso;
        this.temporadas = temporadas;
        this.pontos = pontos;
        this.trofeus = trofeus;
        this.tentativas = tentativas;
        this.respostas = respostas;
        this.sequencias = sequencias;
        this.leitor = leitor;
        this.regras = regras;
        this.aleatorio = aleatorio;
        this.relogio = relogio;
    }

    @Transactional
    public TentativaIniciada iniciar(Long colaboradorId, Long licaoId) {
        ConteudoPublicado.Licao licao = conteudo.licao(licaoId);
        if (!progresso.desbloqueada(colaboradorId, licaoId)) {
            throw new ErroDeNegocio("Conclua as lições anteriores para liberar esta.");
        }
        List<DesafioEmJogo> desafios = conteudo.desafios(licaoId);

        tentativas
                .findByColaboradorIdAndLicaoIdAndStatus(colaboradorId, licaoId, StatusDaTentativa.EM_ANDAMENTO)
                .forEach(Tentativa::abandonar);
        long conclusoesAnteriores =
                tentativas.countByColaboradorIdAndLicaoIdAndStatus(colaboradorId, licaoId, StatusDaTentativa.CONCLUIDA);
        Long temporadaId =
                temporadas.ativa().map(ServicoDeTemporadas.TemporadaVista::id).orElse(null);

        Tentativa tentativa = tentativas.save(new Tentativa(
                colaboradorId,
                licaoId,
                temporadaId,
                relogio.instant(),
                desafios.size(),
                RegrasDePontuacao.percentualAoRefazer(conclusoesAnteriores)));

        return new TentativaIniciada(
                tentativa.getId(),
                licao.titulo(),
                tentativa.getPercentualPontos() == RegrasDePontuacao.PERCENTUAL_INTEGRAL,
                desafios.stream().map(desafio -> desafio.publico(aleatorio)).toList());
    }

    @Transactional
    public RespostaCorrigida responder(Long colaboradorId, Long tentativaId, Long desafioId, JsonNode respostaJson) {
        Tentativa tentativa = tentativaDoColaborador(colaboradorId, tentativaId);
        tentativa.exigirEmAndamento();
        DesafioEmJogo desafio = conteudo.desafios(tentativa.getLicaoId()).stream()
                .filter(candidato -> candidato.id().equals(desafioId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontrado("Esse desafio não faz parte da lição."));

        List<Resposta> anteriores = respostas.findByTentativaIdOrderByIdAsc(tentativaId);
        boolean segundaChance = exigirQuePodeResponder(tentativa, anteriores, desafioId);

        RespostaDoColaborador resposta = leitor.lerResposta(desafio.tipo(), respostaJson);
        Correcao correcao = desafio.conteudo().corrigir(resposta);

        PontosDoAcerto pontosDoAcerto = new PontosDoAcerto(0, 0);
        if (correcao.correta() && !segundaChance) {
            pontosDoAcerto = regras.pontosPorAcerto(
                    desafio.pontos(),
                    desafio.dificuldade(),
                    acertosSeguidosAoFinal(anteriores) + 1,
                    tentativa.getPercentualPontos());
            tentativa.registrarAcerto();
        }

        Resposta registrada = respostas.save(new Resposta(
                tentativaId,
                desafioId,
                desafio.versao(),
                leitor.escrever(resposta),
                correcao.correta(),
                pontosDoAcerto.total(),
                segundaChance,
                relogio.instant()));
        List<Resposta> todas = new ArrayList<>(anteriores);
        todas.add(registrada);

        return new RespostaCorrigida(
                correcao.correta(),
                correcao.motivoDoErro(),
                correcao.respostaCerta(),
                desafio.explicacao(),
                pontosDoAcerto.base(),
                pontosDoAcerto.bonusDeSequencia(),
                segundaChance,
                pendentesDeSegundaChance(tentativa, todas));
    }

    @Transactional
    public ResultadoDaLicao concluir(Long colaboradorId, Long tentativaId) {
        Tentativa tentativa = tentativaDoColaborador(colaboradorId, tentativaId);
        tentativa.exigirEmAndamento();
        List<Resposta> lista = respostas.findByTentativaIdOrderByIdAsc(tentativaId);
        List<Resposta> primeiras =
                lista.stream().filter(Resposta::primeiraResposta).toList();
        if (primeiras.size() < tentativa.getTotal()) {
            throw new ErroDeNegocio("Responda todos os desafios antes de concluir a lição.");
        }

        Instant agora = relogio.instant();
        LocalDate hoje = LocalDate.ofInstant(agora, relogio.getZone());
        boolean perfeita = primeiras.stream().allMatch(Resposta::isCorreta);
        boolean primeiraDoDia = !tentativas.existsByColaboradorIdAndStatusAndConcluidaEmBetween(
                colaboradorId,
                StatusDaTentativa.CONCLUIDA,
                hoje.atStartOfDay(relogio.getZone()).toInstant(),
                hoje.plusDays(1).atStartOfDay(relogio.getZone()).toInstant().minusNanos(1));
        tentativa.concluir(agora, perfeita);

        List<Ganho> ganhos = new ArrayList<>();
        int pontosDosAcertos = primeiras.stream().mapToInt(Resposta::getPontos).sum();
        if (pontosDosAcertos > 0) {
            ganhos.add(new Ganho(MotivoDosPontos.ACERTOS, pontosDosAcertos));
        }
        ganhos.addAll(regras.bonusDeConclusao(perfeita, primeiraDoDia, tentativa.getPercentualPontos()));
        pontos.registrar(colaboradorId, tentativa.getTemporadaId(), tentativaId, ganhos, agora);

        SequenciaDeDias sequencia = registrarDiaDeEstudo(colaboradorId, hoje);
        tentativas.flush();

        Long moduloId = conteudo.licao(tentativa.getLicaoId()).moduloId();
        boolean moduloConcluido = progresso.moduloConcluido(colaboradorId, moduloId);
        boolean trilhaConcluida = moduloConcluido && progresso.trilhaConcluida(colaboradorId);
        List<TrofeuDoColaborador> novos = trofeus.conceder(
                colaboradorId,
                new RegrasDeTrofeus.Situacao(true, perfeita, sequencia.diasAtuais(), moduloConcluido, trilhaConcluida),
                agora);

        return new ResultadoDaLicao(
                tentativa.getAcertos(),
                tentativa.getTotal(),
                perfeita,
                tentativa.getTempoMs(),
                ganhos,
                ganhos.stream().mapToInt(Ganho::pontos).sum(),
                sequencia.diasAtuais(),
                novos,
                moduloConcluido
                        ? new ModuloConcluido(
                                moduloId, conteudo.modulo(moduloId).titulo())
                        : null);
    }

    private Tentativa tentativaDoColaborador(Long colaboradorId, Long tentativaId) {
        return tentativas
                .findByIdAndColaboradorId(tentativaId, colaboradorId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Lição em andamento não encontrada."));
    }

    private static boolean exigirQuePodeResponder(Tentativa tentativa, List<Resposta> anteriores, Long desafioId) {
        Resposta primeira = anteriores.stream()
                .filter(Resposta::primeiraResposta)
                .filter(resposta -> resposta.getDesafioId().equals(desafioId))
                .findFirst()
                .orElse(null);
        if (primeira == null) {
            return false;
        }
        boolean todasRespondidas =
                anteriores.stream().filter(Resposta::primeiraResposta).count() == tentativa.getTotal();
        boolean jaUsouSegundaChance = anteriores.stream()
                .anyMatch(resposta ->
                        resposta.isSegundaChance() && resposta.getDesafioId().equals(desafioId));
        if (primeira.isCorreta() || !todasRespondidas || jaUsouSegundaChance) {
            throw new ErroDeNegocio("Esse desafio já foi respondido.");
        }
        return true;
    }

    private static int acertosSeguidosAoFinal(List<Resposta> anteriores) {
        List<Resposta> primeiras =
                anteriores.stream().filter(Resposta::primeiraResposta).toList();
        int seguidos = 0;
        for (int indice = primeiras.size() - 1;
                indice >= 0 && primeiras.get(indice).isCorreta();
                indice--) {
            seguidos++;
        }
        return seguidos;
    }

    private static List<Long> pendentesDeSegundaChance(Tentativa tentativa, List<Resposta> todas) {
        List<Resposta> primeiras =
                todas.stream().filter(Resposta::primeiraResposta).toList();
        if (primeiras.size() < tentativa.getTotal()) {
            return List.of();
        }
        List<Long> comSegundaChance = todas.stream()
                .filter(Resposta::isSegundaChance)
                .map(Resposta::getDesafioId)
                .toList();
        return primeiras.stream()
                .filter(resposta -> !resposta.isCorreta())
                .map(Resposta::getDesafioId)
                .filter(desafioId -> !comSegundaChance.contains(desafioId))
                .toList();
    }

    private SequenciaDeDias registrarDiaDeEstudo(Long colaboradorId, LocalDate hoje) {
        return sequencias
                .findById(colaboradorId)
                .map(salva -> {
                    SequenciaDeDias atualizada = salva.paraDominio().registrar(hoje);
                    salva.aplicar(atualizada);
                    return atualizada;
                })
                .orElseGet(() -> {
                    SequenciaDeDias primeira = SequenciaDeDias.primeiroDia(hoje);
                    sequencias.save(new SequenciaDiaria(colaboradorId, primeira));
                    return primeira;
                });
    }
}

package br.com.unimed.uniquiz.classificacao.aplicacao;

import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeTemporadas.TemporadaVista;
import br.com.unimed.uniquiz.classificacao.persistencia.ConsultasDeClassificacao;
import br.com.unimed.uniquiz.classificacao.persistencia.ConsultasDeClassificacao.Linha;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeClassificacao {

    static final int TAMANHO_DO_TOPO = 10;

    public record Posicao(int posicao, String nome, String setor, long valor, boolean souEu) {}

    public record Classificacao(
            TemporadaVista temporada, List<Posicao> topo, Posicao minhaPosicao, Long pontosParaSubir) {

        static Classificacao semTemporada() {
            return new Classificacao(null, List.of(), null, null);
        }
    }

    private final ConsultasDeClassificacao consultas;
    private final ServicoDeTemporadas temporadas;

    public ServicoDeClassificacao(ConsultasDeClassificacao consultas, ServicoDeTemporadas temporadas) {
        this.consultas = consultas;
        this.temporadas = temporadas;
    }

    @Transactional(readOnly = true)
    public Classificacao porPontos(Long colaboradorId, Long temporadaId) {
        Optional<TemporadaVista> temporada =
                temporadaId == null ? temporadas.ativa() : Optional.of(temporadas.buscar(temporadaId));
        if (temporada.isEmpty()) {
            return Classificacao.semTemporada();
        }
        List<Linha> linhas = consultas.porPontos(temporada.get().id(), colaboradorId, TAMANHO_DO_TOPO);
        Optional<Linha> minha = linhas.stream()
                .filter(linha -> linha.colaboradorId() == colaboradorId)
                .findFirst();
        Long pontosParaSubir = minha.filter(linha -> linha.posicao() > 1)
                .flatMap(linha -> linhas.stream()
                        .filter(outra -> outra.posicao() == linha.posicao() - 1)
                        .findFirst()
                        .map(acima -> acima.valor() - linha.valor() + 1))
                .orElse(null);
        return new Classificacao(
                temporada.get(),
                topo(linhas, colaboradorId),
                minha.map(linha -> paraPosicao(linha, colaboradorId)).orElse(null),
                pontosParaSubir);
    }

    @Transactional(readOnly = true)
    public Classificacao maisRapidos(Long colaboradorId, Long licaoId) {
        Optional<TemporadaVista> temporada = temporadas.ativa();
        if (temporada.isEmpty()) {
            return Classificacao.semTemporada();
        }
        List<Linha> linhas = consultas.maisRapidos(temporada.get().id(), licaoId, colaboradorId, TAMANHO_DO_TOPO);
        Posicao minha = linhas.stream()
                .filter(linha -> linha.colaboradorId() == colaboradorId)
                .findFirst()
                .map(linha -> paraPosicao(linha, colaboradorId))
                .orElse(null);
        return new Classificacao(temporada.get(), topo(linhas, colaboradorId), minha, null);
    }

    private static List<Posicao> topo(List<Linha> linhas, long colaboradorId) {
        return linhas.stream()
                .filter(linha -> linha.posicao() <= TAMANHO_DO_TOPO)
                .map(linha -> paraPosicao(linha, colaboradorId))
                .toList();
    }

    private static Posicao paraPosicao(Linha linha, long colaboradorId) {
        return new Posicao(
                linha.posicao(), linha.nome(), linha.setor(), linha.valor(), linha.colaboradorId() == colaboradorId);
    }
}

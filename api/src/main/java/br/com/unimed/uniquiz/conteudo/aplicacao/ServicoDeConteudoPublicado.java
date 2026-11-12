package br.com.unimed.uniquiz.conteudo.aplicacao;

import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoPublicado.DesafioEmJogo;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import br.com.unimed.uniquiz.conteudo.persistencia.Desafio;
import br.com.unimed.uniquiz.conteudo.persistencia.DesafioRepository;
import br.com.unimed.uniquiz.conteudo.persistencia.Licao;
import br.com.unimed.uniquiz.conteudo.persistencia.LicaoRepository;
import br.com.unimed.uniquiz.conteudo.persistencia.Modulo;
import br.com.unimed.uniquiz.conteudo.persistencia.ModuloRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeConteudoPublicado {

    private final ModuloRepository modulos;
    private final LicaoRepository licoes;
    private final DesafioRepository desafios;
    private final LeitorDeConteudo leitor;

    public ServicoDeConteudoPublicado(
            ModuloRepository modulos, LicaoRepository licoes, DesafioRepository desafios, LeitorDeConteudo leitor) {
        this.modulos = modulos;
        this.licoes = licoes;
        this.desafios = desafios;
        this.leitor = leitor;
    }

    @Transactional(readOnly = true)
    public List<ConteudoPublicado.Modulo> trilha() {
        List<Modulo> publicados = modulos.findByStatusOrderByOrdemAsc(StatusPublicacao.PUBLICADO);
        if (publicados.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ConteudoPublicado.Licao>> licoesPorModulo = licoes
                .findByModuloIdInAndStatusOrderByOrdemAsc(
                        publicados.stream().map(Modulo::getId).toList(), StatusPublicacao.PUBLICADO)
                .stream()
                .map(ServicoDeConteudoPublicado::publicada)
                .collect(Collectors.groupingBy(ConteudoPublicado.Licao::moduloId));
        return publicados.stream()
                .map(modulo -> new ConteudoPublicado.Modulo(
                        modulo.getId(),
                        modulo.getTitulo(),
                        modulo.getDescricao(),
                        modulo.getMaterialEstudo() != null,
                        licoesPorModulo.getOrDefault(modulo.getId(), List.of())))
                .filter(modulo -> !modulo.licoes().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public ConteudoPublicado.Modulo modulo(Long moduloId) {
        return trilha().stream()
                .filter(modulo -> modulo.id().equals(moduloId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontrado("Módulo não encontrado."));
    }

    @Transactional(readOnly = true)
    public ConteudoPublicado.Material material(Long moduloId) {
        Modulo modulo = modulos.findById(moduloId)
                .filter(Modulo::publicado)
                .orElseThrow(() -> new RecursoNaoEncontrado("Módulo não encontrado."));
        if (modulo.getMaterialEstudo() == null) {
            throw new RecursoNaoEncontrado("Este módulo ainda não tem material de estudo.");
        }
        return new ConteudoPublicado.Material(modulo.getId(), modulo.getTitulo(), modulo.getMaterialEstudo());
    }

    @Transactional(readOnly = true)
    public ConteudoPublicado.Licao licao(Long licaoId) {
        Licao licao = licoes.findById(licaoId)
                .filter(Licao::publicada)
                .filter(encontrada -> modulos.findById(encontrada.getModuloId())
                        .map(Modulo::publicado)
                        .orElse(false))
                .orElseThrow(() -> new RecursoNaoEncontrado("Lição não encontrada."));
        return publicada(licao);
    }

    @Transactional(readOnly = true)
    public List<DesafioEmJogo> desafios(Long licaoId) {
        licao(licaoId);
        return desafios.findByLicaoIdAndStatusOrderByOrdemAsc(licaoId, StatusPublicacao.PUBLICADO).stream()
                .map(this::emJogo)
                .toList();
    }

    private DesafioEmJogo emJogo(Desafio desafio) {
        return new DesafioEmJogo(
                desafio.getId(),
                desafio.getLicaoId(),
                desafio.getVersao(),
                desafio.getTipo(),
                desafio.getEnunciado(),
                desafio.getDica(),
                desafio.getDificuldade(),
                desafio.getPontos(),
                desafio.getExplicacao(),
                leitor.ler(desafio.getTipo(), desafio.getConteudoJson()));
    }

    private static ConteudoPublicado.Licao publicada(Licao licao) {
        return new ConteudoPublicado.Licao(licao.getId(), licao.getModuloId(), licao.getTitulo(), licao.quantidades());
    }
}

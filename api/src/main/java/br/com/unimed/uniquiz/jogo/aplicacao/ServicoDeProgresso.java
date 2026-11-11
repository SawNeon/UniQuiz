package br.com.unimed.uniquiz.jogo.aplicacao;

import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoPublicado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ServicoDeConteudoPublicado;
import br.com.unimed.uniquiz.jogo.dominio.ProgressoNaTrilha;
import br.com.unimed.uniquiz.jogo.dominio.ProgressoNaTrilha.SituacaoDaLicao;
import br.com.unimed.uniquiz.jogo.dominio.SequenciaDeDias;
import br.com.unimed.uniquiz.jogo.dominio.StatusDaTentativa;
import br.com.unimed.uniquiz.jogo.persistencia.SequenciaDiaria;
import br.com.unimed.uniquiz.jogo.persistencia.SequenciaDiariaRepository;
import br.com.unimed.uniquiz.jogo.persistencia.TentativaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeProgresso {

    public record LicaoNoCaminho(Long id, String titulo, SituacaoDaLicao situacao) {}

    public record ModuloNoCaminho(
            Long id,
            String titulo,
            String descricao,
            boolean temMaterial,
            boolean concluido,
            int licoesConcluidas,
            List<LicaoNoCaminho> licoes) {}

    private final ServicoDeConteudoPublicado conteudo;
    private final TentativaRepository tentativas;
    private final SequenciaDiariaRepository sequencias;

    public ServicoDeProgresso(
            ServicoDeConteudoPublicado conteudo, TentativaRepository tentativas, SequenciaDiariaRepository sequencias) {
        this.conteudo = conteudo;
        this.tentativas = tentativas;
        this.sequencias = sequencias;
    }

    @Transactional(readOnly = true)
    public List<ModuloNoCaminho> caminho(Long colaboradorId) {
        List<ConteudoPublicado.Modulo> trilha = conteudo.trilha();
        Map<Long, SituacaoDaLicao> situacoes = situacoes(trilha, colaboradorId);
        return trilha.stream()
                .map(modulo -> {
                    List<LicaoNoCaminho> licoes = modulo.licoes().stream()
                            .map(licao -> new LicaoNoCaminho(licao.id(), licao.titulo(), situacoes.get(licao.id())))
                            .toList();
                    int concluidas = (int) licoes.stream()
                            .filter(licao -> licao.situacao() == SituacaoDaLicao.CONCLUIDA)
                            .count();
                    return new ModuloNoCaminho(
                            modulo.id(),
                            modulo.titulo(),
                            modulo.descricao(),
                            modulo.temMaterial(),
                            concluidas == licoes.size(),
                            concluidas,
                            licoes);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SequenciaDeDias> sequencia(Long colaboradorId) {
        return sequencias.findById(colaboradorId).map(SequenciaDiaria::paraDominio);
    }

    @Transactional(readOnly = true)
    public boolean desbloqueada(Long colaboradorId, Long licaoId) {
        SituacaoDaLicao situacao = situacoes(conteudo.trilha(), colaboradorId).get(licaoId);
        return situacao != null && situacao != SituacaoDaLicao.BLOQUEADA;
    }

    @Transactional(readOnly = true)
    public boolean moduloConcluido(Long colaboradorId, Long moduloId) {
        return caminho(colaboradorId).stream().anyMatch(modulo -> modulo.id().equals(moduloId) && modulo.concluido());
    }

    @Transactional(readOnly = true)
    public boolean trilhaConcluida(Long colaboradorId) {
        List<ModuloNoCaminho> caminho = caminho(colaboradorId);
        return !caminho.isEmpty() && caminho.stream().allMatch(ModuloNoCaminho::concluido);
    }

    private Map<Long, SituacaoDaLicao> situacoes(List<ConteudoPublicado.Modulo> trilha, Long colaboradorId) {
        List<Long> emOrdem = trilha.stream()
                .flatMap(modulo -> modulo.licoes().stream())
                .map(ConteudoPublicado.Licao::id)
                .toList();
        Set<Long> concluidas = tentativas.licoesComStatus(colaboradorId, StatusDaTentativa.CONCLUIDA);
        return ProgressoNaTrilha.situacoes(emOrdem, concluidas);
    }
}

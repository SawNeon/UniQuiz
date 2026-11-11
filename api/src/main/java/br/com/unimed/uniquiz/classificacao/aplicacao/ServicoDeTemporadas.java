package br.com.unimed.uniquiz.classificacao.aplicacao;

import br.com.unimed.uniquiz.classificacao.persistencia.Temporada;
import br.com.unimed.uniquiz.classificacao.persistencia.TemporadaRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeTemporadas {

    public record DadosDaTemporada(
            @NotBlank @Size(max = 100) String nome,
            @NotNull LocalDate inicio,
            @NotNull LocalDate fim,
            @Size(max = 300) String premioTexto) {}

    public record TemporadaVista(Long id, String nome, LocalDate inicio, LocalDate fim, String premioTexto) {

        static TemporadaVista de(Temporada temporada) {
            return new TemporadaVista(
                    temporada.getId(),
                    temporada.getNome(),
                    temporada.getInicio(),
                    temporada.getFim(),
                    temporada.getPremioTexto());
        }
    }

    private final TemporadaRepository temporadas;
    private final Clock relogio;

    public ServicoDeTemporadas(TemporadaRepository temporadas, Clock relogio) {
        this.temporadas = temporadas;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Optional<TemporadaVista> ativa() {
        LocalDate hoje = LocalDate.now(relogio);
        return temporadas.findByInicioLessThanEqualAndFimGreaterThanEqualOrderByInicioDesc(hoje, hoje).stream()
                .findFirst()
                .map(TemporadaVista::de);
    }

    @Transactional(readOnly = true)
    public TemporadaVista buscar(Long temporadaId) {
        return temporadas.findById(temporadaId)
                .map(TemporadaVista::de)
                .orElseThrow(() -> new RecursoNaoEncontrado("Temporada não encontrada."));
    }

    @Transactional(readOnly = true)
    public List<TemporadaVista> listar() {
        return temporadas.findAllByOrderByInicioDesc().stream().map(TemporadaVista::de).toList();
    }

    @Transactional
    public TemporadaVista criar(DadosDaTemporada dados) {
        exigirSemSobreposicao(null, dados);
        Temporada temporada =
                temporadas.save(new Temporada(dados.nome(), dados.inicio(), dados.fim(), dados.premioTexto()));
        return TemporadaVista.de(temporada);
    }

    @Transactional
    public TemporadaVista atualizar(Long temporadaId, DadosDaTemporada dados) {
        Temporada temporada = temporadas.findById(temporadaId)
                .orElseThrow(() -> new RecursoNaoEncontrado("Temporada não encontrada."));
        exigirSemSobreposicao(temporadaId, dados);
        temporada.atualizar(dados.nome(), dados.inicio(), dados.fim(), dados.premioTexto());
        return TemporadaVista.de(temporada);
    }

    private void exigirSemSobreposicao(Long ignorar, DadosDaTemporada dados) {
        boolean sobrepoe = temporadas.findAll().stream()
                .filter(outra -> !outra.getId().equals(ignorar))
                .anyMatch(outra -> outra.sobrepoe(dados.inicio(), dados.fim()));
        if (sobrepoe) {
            throw new ErroDeNegocio("As datas coincidem com outra temporada. Ajuste o período.");
        }
    }
}

package br.com.unimed.uniquiz.classificacao.aplicacao;

import br.com.unimed.uniquiz.classificacao.dominio.Ganho;
import br.com.unimed.uniquiz.classificacao.dominio.MotivoDosPontos;
import br.com.unimed.uniquiz.classificacao.persistencia.EventoPontos;
import br.com.unimed.uniquiz.classificacao.persistencia.EventoPontosRepository;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDePontos {

    private final EventoPontosRepository eventos;
    private final ServicoDeTemporadas temporadas;
    private final Clock relogio;

    public ServicoDePontos(EventoPontosRepository eventos, ServicoDeTemporadas temporadas, Clock relogio) {
        this.eventos = eventos;
        this.temporadas = temporadas;
        this.relogio = relogio;
    }

    @Transactional
    public void registrar(Long colaboradorId, Long temporadaId, Long tentativaId, List<Ganho> ganhos, Instant quando) {
        ganhos.stream()
                .filter(ganho -> ganho.pontos() != 0)
                .map(ganho -> new EventoPontos(
                        colaboradorId, temporadaId, tentativaId, ganho.motivo(), ganho.pontos(), quando))
                .forEach(eventos::save);
    }

    @Transactional(readOnly = true)
    public int total(Long colaboradorId) {
        return eventos.totalDoColaborador(colaboradorId);
    }

    @Transactional(readOnly = true)
    public int naTemporada(Long colaboradorId, Long temporadaId) {
        return eventos.totalNaTemporada(colaboradorId, temporadaId);
    }

    @Transactional
    public void zerarNaTemporadaAtiva(Long colaboradorId) {
        Long temporadaId = temporadas
                .ativa()
                .map(ServicoDeTemporadas.TemporadaVista::id)
                .orElseThrow(() -> new ErroDeNegocio("Não há temporada ativa para zerar os pontos."));
        int pontos = eventos.totalNaTemporada(colaboradorId, temporadaId);
        if (pontos != 0) {
            eventos.save(new EventoPontos(
                    colaboradorId, temporadaId, null, MotivoDosPontos.AJUSTE_DO_ADMIN, -pontos, relogio.instant()));
        }
    }
}

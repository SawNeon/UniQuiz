package br.com.unimed.uniquiz.trofeu.aplicacao;

import br.com.unimed.uniquiz.trofeu.dominio.CodigoDoTrofeu;
import br.com.unimed.uniquiz.trofeu.dominio.RegrasDeTrofeus;
import br.com.unimed.uniquiz.trofeu.persistencia.ColaboradorTrofeu;
import br.com.unimed.uniquiz.trofeu.persistencia.ColaboradorTrofeuRepository;
import br.com.unimed.uniquiz.trofeu.persistencia.Trofeu;
import br.com.unimed.uniquiz.trofeu.persistencia.TrofeuRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeTrofeus {

    public record TrofeuDoColaborador(
            CodigoDoTrofeu codigo, String nome, String descricao, String icone, Instant conquistadoEm) {}

    private final TrofeuRepository catalogo;
    private final ColaboradorTrofeuRepository conquistas;

    public ServicoDeTrofeus(TrofeuRepository catalogo, ColaboradorTrofeuRepository conquistas) {
        this.catalogo = catalogo;
        this.conquistas = conquistas;
    }

    @Transactional
    public List<TrofeuDoColaborador> conceder(Long colaboradorId, RegrasDeTrofeus.Situacao situacao, Instant agora) {
        Set<CodigoDoTrofeu> merecidos = RegrasDeTrofeus.merecidos(situacao);
        Set<Long> jaConquistados = conquistas.findByColaboradorId(colaboradorId).stream()
                .map(ColaboradorTrofeu::getTrofeuId)
                .collect(Collectors.toSet());
        return catalogo.findAllByOrderByIdAsc().stream()
                .filter(trofeu -> merecidos.contains(trofeu.getCodigo()))
                .filter(trofeu -> !jaConquistados.contains(trofeu.getId()))
                .map(trofeu -> {
                    conquistas.save(new ColaboradorTrofeu(colaboradorId, trofeu.getId(), agora));
                    return vista(trofeu, agora);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrofeuDoColaborador> doColaborador(Long colaboradorId) {
        Map<Long, Instant> conquistados = conquistas.findByColaboradorId(colaboradorId).stream()
                .collect(Collectors.toMap(ColaboradorTrofeu::getTrofeuId, ColaboradorTrofeu::getConquistadoEm));
        return catalogo.findAllByOrderByIdAsc().stream()
                .map(trofeu -> vista(trofeu, conquistados.get(trofeu.getId())))
                .toList();
    }

    private static TrofeuDoColaborador vista(Trofeu trofeu, Instant conquistadoEm) {
        return new TrofeuDoColaborador(
                trofeu.getCodigo(), trofeu.getNome(), trofeu.getDescricao(), trofeu.getIcone(), conquistadoEm);
    }
}

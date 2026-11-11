package br.com.unimed.uniquiz.perfil.aplicacao;

import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificados;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificados.CertificadoVista;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDePontos;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeTemporadas;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores.Identificacao;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeProgresso;
import br.com.unimed.uniquiz.jogo.dominio.RegrasDePontuacao;
import br.com.unimed.uniquiz.jogo.dominio.SequenciaDeDias;
import br.com.unimed.uniquiz.trofeu.aplicacao.ServicoDeTrofeus;
import br.com.unimed.uniquiz.trofeu.aplicacao.ServicoDeTrofeus.TrofeuDoColaborador;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDePerfil {

    public record Perfil(
            String nome,
            String setor,
            String empresa,
            int pontosNaTemporada,
            int pontosTotais,
            int nivel,
            int pontosParaProximoNivel,
            int diasSeguidos,
            int maiorSequencia,
            List<TrofeuDoColaborador> trofeus,
            List<CertificadoVista> certificados) {}

    private final ServicoDeColaboradores colaboradores;
    private final ServicoDePontos pontos;
    private final ServicoDeTemporadas temporadas;
    private final ServicoDeProgresso progresso;
    private final ServicoDeTrofeus trofeus;
    private final ServicoDeCertificados certificados;
    private final RegrasDePontuacao regras;
    private final Clock relogio;

    public ServicoDePerfil(
            ServicoDeColaboradores colaboradores,
            ServicoDePontos pontos,
            ServicoDeTemporadas temporadas,
            ServicoDeProgresso progresso,
            ServicoDeTrofeus trofeus,
            ServicoDeCertificados certificados,
            RegrasDePontuacao regras,
            Clock relogio) {
        this.colaboradores = colaboradores;
        this.pontos = pontos;
        this.temporadas = temporadas;
        this.progresso = progresso;
        this.trofeus = trofeus;
        this.certificados = certificados;
        this.regras = regras;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Perfil doColaborador(Long colaboradorId) {
        Identificacao identificacao = colaboradores.identificar(colaboradorId);
        int total = pontos.total(colaboradorId);
        int naTemporada = temporadas
                .ativa()
                .map(temporada -> pontos.naTemporada(colaboradorId, temporada.id()))
                .orElse(0);
        int nivel = regras.nivel(total);
        Optional<SequenciaDeDias> sequencia = progresso.sequencia(colaboradorId);
        LocalDate hoje = LocalDate.now(relogio);

        return new Perfil(
                identificacao.nome(),
                identificacao.setor(),
                identificacao.empresa(),
                naTemporada,
                total,
                nivel,
                nivel * regras.pontosPorNivel() - Math.max(total, 0),
                sequencia.map(dias -> dias.diasVigentes(hoje)).orElse(0),
                sequencia.map(SequenciaDeDias::maiorSequencia).orElse(0),
                trofeus.doColaborador(colaboradorId),
                certificados.doColaborador(colaboradorId));
    }
}

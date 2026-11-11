package br.com.unimed.uniquiz.certificado.aplicacao;

import br.com.unimed.uniquiz.certificado.dominio.CodigoDeValidacao;
import br.com.unimed.uniquiz.certificado.persistencia.Certificado;
import br.com.unimed.uniquiz.certificado.persistencia.CertificadoRepository;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.conteudo.aplicacao.ServicoDeConteudoPublicado;
import br.com.unimed.uniquiz.jogo.aplicacao.ServicoDeProgresso;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeCertificados {

    private static final String CERTIFICADO_NAO_ENCONTRADO = "Certificado não encontrado.";

    public record CertificadoVista(String codigo, Long moduloId, String modulo, LocalDate emitidoEm) {}

    public record Validacao(String codigo, String nome, String modulo, LocalDate emitidoEm) {}

    public record ArquivoPdf(String nome, byte[] conteudo) {}

    private final CertificadoRepository certificados;
    private final ServicoDeProgresso progresso;
    private final ServicoDeConteudoPublicado conteudo;
    private final ServicoDeColaboradores colaboradores;
    private final GeradorDePdf gerador;
    private final RandomGenerator aleatorio;
    private final Clock relogio;
    private final String urlPublica;

    public ServicoDeCertificados(
            CertificadoRepository certificados,
            ServicoDeProgresso progresso,
            ServicoDeConteudoPublicado conteudo,
            ServicoDeColaboradores colaboradores,
            GeradorDePdf gerador,
            RandomGenerator aleatorio,
            Clock relogio,
            PropriedadesDoUniQuiz propriedades) {
        this.certificados = certificados;
        this.progresso = progresso;
        this.conteudo = conteudo;
        this.colaboradores = colaboradores;
        this.gerador = gerador;
        this.aleatorio = aleatorio;
        this.relogio = relogio;
        this.urlPublica = propriedades.urlPublica();
    }

    @Transactional(readOnly = true)
    public List<CertificadoVista> doColaborador(Long colaboradorId) {
        return certificados.findByColaboradorIdOrderByEmitidoEmDesc(colaboradorId).stream()
                .map(this::vista)
                .toList();
    }

    @Transactional
    public CertificadoVista emitir(Long colaboradorId, Long moduloId) {
        return certificados
                .findByColaboradorIdAndModuloId(colaboradorId, moduloId)
                .map(this::vista)
                .orElseGet(() -> vista(novo(colaboradorId, moduloId)));
    }

    @Transactional(readOnly = true)
    public ArquivoPdf pdf(Long colaboradorId, String codigo) {
        Certificado certificado = certificados
                .findByCodigo(CodigoDeValidacao.normalizar(codigo))
                .filter(encontrado -> encontrado.getColaboradorId().equals(colaboradorId))
                .orElseThrow(() -> new RecursoNaoEncontrado(CERTIFICADO_NAO_ENCONTRADO));
        byte[] conteudoDoPdf = gerador.gerar(new GeradorDePdf.DadosDoCertificado(
                certificado.getNomeColaborador(),
                certificado.getTituloModulo(),
                dataDeEmissao(certificado),
                certificado.getCodigo(),
                urlPublica + "/certificado/" + certificado.getCodigo()));
        return new ArquivoPdf("certificado-uniquiz-" + certificado.getCodigo() + ".pdf", conteudoDoPdf);
    }

    @Transactional(readOnly = true)
    public Validacao validar(String codigoDigitado) {
        String codigo = CodigoDeValidacao.normalizar(codigoDigitado);
        if (!CodigoDeValidacao.valido(codigo)) {
            throw new RecursoNaoEncontrado(CERTIFICADO_NAO_ENCONTRADO);
        }
        return certificados
                .findByCodigo(codigo)
                .map(certificado -> new Validacao(
                        CodigoDeValidacao.formatado(certificado.getCodigo()),
                        certificado.getNomeColaborador(),
                        certificado.getTituloModulo(),
                        dataDeEmissao(certificado)))
                .orElseThrow(() -> new RecursoNaoEncontrado(CERTIFICADO_NAO_ENCONTRADO));
    }

    private Certificado novo(Long colaboradorId, Long moduloId) {
        if (!progresso.moduloConcluido(colaboradorId, moduloId)) {
            throw new ErroDeNegocio("Conclua todas as lições do módulo para receber o certificado.");
        }
        String codigo;
        do {
            codigo = CodigoDeValidacao.gerar(aleatorio);
        } while (certificados.existsByCodigo(codigo));

        return certificados.save(new Certificado(
                codigo,
                colaboradorId,
                moduloId,
                colaboradores.identificar(colaboradorId).nome(),
                conteudo.modulo(moduloId).titulo(),
                relogio.instant()));
    }

    private CertificadoVista vista(Certificado certificado) {
        return new CertificadoVista(
                certificado.getCodigo(),
                certificado.getModuloId(),
                certificado.getTituloModulo(),
                dataDeEmissao(certificado));
    }

    private LocalDate dataDeEmissao(Certificado certificado) {
        return LocalDate.ofInstant(certificado.getEmitidoEm(), relogio.getZone());
    }
}

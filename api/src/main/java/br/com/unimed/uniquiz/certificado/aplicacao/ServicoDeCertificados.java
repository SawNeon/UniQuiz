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
import java.util.Set;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeCertificados {

    private static final String CERTIFICADO_NAO_ENCONTRADO = "Certificado não encontrado.";
    private static final String AINDA_NAO_LIBERADO =
            "Seu certificado está garantido, mas ainda não foi liberado. Assim que for, ele aparece aqui para baixar.";

    public record CertificadoVista(
            String codigo, Long moduloId, String modulo, LocalDate emitidoEm, boolean liberado) {}

    public record Validacao(String codigo, String nome, String modulo, LocalDate emitidoEm) {}

    public record ArquivoPdf(String nome, byte[] conteudo) {}

    private final CertificadoRepository certificados;
    private final ServicoDeProgresso progresso;
    private final ServicoDeConteudoPublicado conteudo;
    private final ServicoDeColaboradores colaboradores;
    private final GeradorDePdf gerador;
    private final ServicoDoModeloDeCertificado modelo;
    private final RandomGenerator aleatorio;
    private final Clock relogio;
    private final String urlPublica;

    public ServicoDeCertificados(
            CertificadoRepository certificados,
            ServicoDeProgresso progresso,
            ServicoDeConteudoPublicado conteudo,
            ServicoDeColaboradores colaboradores,
            GeradorDePdf gerador,
            ServicoDoModeloDeCertificado modelo,
            RandomGenerator aleatorio,
            Clock relogio,
            PropriedadesDoUniQuiz propriedades) {
        this.certificados = certificados;
        this.progresso = progresso;
        this.conteudo = conteudo;
        this.colaboradores = colaboradores;
        this.gerador = gerador;
        this.modelo = modelo;
        this.aleatorio = aleatorio;
        this.relogio = relogio;
        this.urlPublica = propriedades.urlPublica();
    }

    @Transactional(readOnly = true)
    public List<CertificadoVista> doColaborador(Long colaboradorId) {
        Set<Long> liberados = conteudo.modulosComCertificadoLiberado();
        return certificados.findByColaboradorIdOrderByEmitidoEmDesc(colaboradorId).stream()
                .map(certificado -> vista(certificado, liberados.contains(certificado.getModuloId())))
                .toList();
    }

    @Transactional
    public CertificadoVista emitir(Long colaboradorId, Long moduloId) {
        Certificado certificado = certificados
                .findByColaboradorIdAndModuloId(colaboradorId, moduloId)
                .orElseGet(() -> {
                    if (!progresso.moduloConcluido(colaboradorId, moduloId)) {
                        throw new ErroDeNegocio("Conclua todas as lições do módulo para receber o certificado.");
                    }
                    return novo(colaboradorId, moduloId);
                });
        return vista(certificado, conteudo.certificadoLiberado(moduloId));
    }

    @Transactional
    public CertificadoVista emitirNaConclusao(Long colaboradorId, Long moduloId) {
        Certificado certificado = certificados
                .findByColaboradorIdAndModuloId(colaboradorId, moduloId)
                .orElseGet(() -> novo(colaboradorId, moduloId));
        return vista(certificado, conteudo.certificadoLiberado(moduloId));
    }

    @Transactional(readOnly = true)
    public ArquivoPdf pdf(Long colaboradorId, String codigo) {
        Certificado certificado = certificados
                .findByCodigo(CodigoDeValidacao.normalizar(codigo))
                .filter(encontrado -> encontrado.getColaboradorId().equals(colaboradorId))
                .orElseThrow(() -> new RecursoNaoEncontrado(CERTIFICADO_NAO_ENCONTRADO));
        if (!conteudo.certificadoLiberado(certificado.getModuloId())) {
            throw new ErroDeNegocio(AINDA_NAO_LIBERADO);
        }
        return new ArquivoPdf("certificado-uniquiz-" + certificado.getCodigo() + ".pdf", gerarPdf(certificado));
    }

    byte[] gerarPdf(Certificado certificado) {
        return gerarPdf(certificado, modelo.atual());
    }

    byte[] gerarPdf(Certificado certificado, GeradorDePdf.Modelo modeloDoPdf) {
        return gerador.gerar(
                new GeradorDePdf.DadosDoCertificado(
                        certificado.getNomeColaborador(),
                        certificado.getTituloModulo(),
                        dataDeEmissao(certificado),
                        certificado.getCodigo(),
                        urlPublica + "/certificado/" + certificado.getCodigo()),
                modeloDoPdf);
    }

    @Transactional(readOnly = true)
    public Validacao validar(String codigoDigitado) {
        String codigo = CodigoDeValidacao.normalizar(codigoDigitado);
        if (!CodigoDeValidacao.valido(codigo)) {
            throw new RecursoNaoEncontrado(CERTIFICADO_NAO_ENCONTRADO);
        }
        return certificados
                .findByCodigo(codigo)
                .filter(certificado -> conteudo.certificadoLiberado(certificado.getModuloId()))
                .map(certificado -> new Validacao(
                        CodigoDeValidacao.formatado(certificado.getCodigo()),
                        certificado.getNomeColaborador(),
                        certificado.getTituloModulo(),
                        dataDeEmissao(certificado)))
                .orElseThrow(() -> new RecursoNaoEncontrado(CERTIFICADO_NAO_ENCONTRADO));
    }

    private Certificado novo(Long colaboradorId, Long moduloId) {
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

    private CertificadoVista vista(Certificado certificado, boolean liberado) {
        return new CertificadoVista(
                certificado.getCodigo(),
                certificado.getModuloId(),
                certificado.getTituloModulo(),
                dataDeEmissao(certificado),
                liberado);
    }

    LocalDate dataDeEmissao(Certificado certificado) {
        return LocalDate.ofInstant(certificado.getEmitidoEm(), relogio.getZone());
    }
}

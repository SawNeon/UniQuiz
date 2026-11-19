package br.com.unimed.uniquiz.certificado.aplicacao;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.AcaoAuditada;
import br.com.unimed.uniquiz.auditoria.dominio.OcorrenciaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import br.com.unimed.uniquiz.certificado.dominio.CodigoDeValidacao;
import br.com.unimed.uniquiz.certificado.persistencia.ConsultasDeCertificados;
import br.com.unimed.uniquiz.certificado.persistencia.ConsultasDeCertificados.Emitido;
import br.com.unimed.uniquiz.certificado.persistencia.ConsultasDeCertificados.EmitidosPorModulo;
import br.com.unimed.uniquiz.certificado.persistencia.ConsultasDeCertificados.Filtro;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.PropriedadesDoUniQuiz;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.comum.Textos;
import br.com.unimed.uniquiz.conteudo.aplicacao.ServicoDeConteudoAdmin;
import br.com.unimed.uniquiz.conteudo.dominio.StatusPublicacao;
import br.com.unimed.uniquiz.relatorio.dominio.EscritaEmCsv;
import br.com.unimed.uniquiz.relatorio.dominio.Planilha;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ServicoDeCertificadosNoPainel {

    public static final int TAMANHO_DA_PAGINA = 50;
    private static final int LIMITE_DO_NOME_NO_ZIP = 80;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record CertificadoNoPainel(
            String codigo,
            String codigoFormatado,
            long moduloId,
            String modulo,
            String nome,
            String empresa,
            String setor,
            LocalDate emitidoEm) {}

    public record PaginaDeCertificados(List<CertificadoNoPainel> itens, int pagina, int tamanho, long total) {}

    public record ModuloComCertificado(
            long id, String titulo, StatusPublicacao status, boolean liberado, long emitidos) {}

    public record ExportacaoPreparada(String nomeDoArquivo, Filtro filtro, List<Emitido> certificados) {}

    private final ConsultasDeCertificados consultas;
    private final ServicoDeConteudoAdmin conteudo;
    private final ServicoDoModeloDeCertificado modelo;
    private final GeradorDePdf gerador;
    private final ServicoDeAuditoria auditoria;
    private final Clock relogio;
    private final String urlPublica;

    public ServicoDeCertificadosNoPainel(
            ConsultasDeCertificados consultas,
            ServicoDeConteudoAdmin conteudo,
            ServicoDoModeloDeCertificado modelo,
            GeradorDePdf gerador,
            ServicoDeAuditoria auditoria,
            Clock relogio,
            PropriedadesDoUniQuiz propriedades) {
        this.consultas = consultas;
        this.conteudo = conteudo;
        this.modelo = modelo;
        this.gerador = gerador;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.urlPublica = propriedades.urlPublica();
    }

    public PaginaDeCertificados emitidos(EscopoDeSetores escopo, Filtro pedido, int pagina) {
        Filtro filtro = filtroPermitido(escopo, pedido);
        int paginaValida = Math.max(pagina, 0);
        List<CertificadoNoPainel> itens = consultas.buscar(escopo, filtro, paginaValida, TAMANHO_DA_PAGINA).stream()
                .map(this::noPainel)
                .toList();
        return new PaginaDeCertificados(itens, paginaValida, TAMANHO_DA_PAGINA, consultas.contar(escopo, filtro));
    }

    public List<ModuloComCertificado> modulos(EscopoDeSetores escopo) {
        Map<Long, Long> emitidos = consultas.porModulo(escopo).stream()
                .collect(Collectors.toMap(EmitidosPorModulo::moduloId, EmitidosPorModulo::emitidos));
        return conteudo.listarModulos().stream()
                .map(modulo -> new ModuloComCertificado(
                        modulo.id(),
                        modulo.titulo(),
                        modulo.status(),
                        modulo.certificadoLiberado(),
                        emitidos.getOrDefault(modulo.id(), 0L)))
                .toList();
    }

    public void liberar(Long moduloId, boolean liberado) {
        conteudo.liberarCertificado(moduloId, liberado);
    }

    public ExportacaoPreparada prepararExportacao(EscopoDeSetores escopo, Filtro pedido) {
        Filtro filtro = filtroPermitido(escopo, pedido);
        List<Emitido> certificados = consultas.todos(escopo, filtro);
        if (certificados.isEmpty()) {
            throw new ErroDeNegocio("Nenhum certificado emitido com esses filtros.");
        }
        return new ExportacaoPreparada(
                "uniquiz-certificados-%s.zip".formatted(LocalDate.now(relogio)), filtro, certificados);
    }

    public void exportar(UsuarioLogado usuario, ExportacaoPreparada exportacao, OutputStream destino)
            throws IOException {
        GeradorDePdf.Modelo modeloDoPdf = modelo.atual();
        ZipOutputStream zip = new ZipOutputStream(destino);
        zip.setLevel(Deflater.BEST_SPEED);
        List<List<Object>> lista = new ArrayList<>();
        for (Emitido certificado : exportacao.certificados()) {
            zip.putNextEntry(new ZipEntry(caminhoNoZip(certificado)));
            zip.write(gerador.gerar(
                    new GeradorDePdf.DadosDoCertificado(
                            certificado.nome(),
                            certificado.modulo(),
                            data(certificado.emitidoEm()),
                            certificado.codigo(),
                            urlPublica + "/certificado/" + certificado.codigo()),
                    modeloDoPdf));
            zip.closeEntry();
            lista.add(new ArrayList<>(List.of(
                    certificado.nome(),
                    certificado.empresa(),
                    certificado.setor(),
                    certificado.modulo(),
                    DATA.format(data(certificado.emitidoEm())),
                    CodigoDeValidacao.formatado(certificado.codigo()),
                    caminhoNoZip(certificado))));
        }
        zip.putNextEntry(new ZipEntry("lista-de-certificados.csv"));
        zip.write(EscritaEmCsv.escrever(new Planilha(
                "Certificados",
                List.of("Nome", "Empresa", "Setor", "Módulo", "Emitido em", "Código de validação", "Arquivo"),
                lista)));
        zip.closeEntry();
        zip.finish();
        zip.flush();
        auditoria.registrar(
                OcorrenciaDeAuditoria.de(TipoDeAtor.ADMINISTRADOR, usuario.id(), AcaoAuditada.CERTIFICADOS_EXPORTADOS)
                        .comAlvo(alvo(exportacao.filtro()))
                        .comDetalhe(exportacao.certificados().size() + " certificados"));
    }

    private Filtro filtroPermitido(EscopoDeSetores escopo, Filtro pedido) {
        if (pedido.setorId() != null && !escopo.permite(pedido.setorId())) {
            throw new RecursoNaoEncontrado("Setor não encontrado.");
        }
        String busca = pedido.busca() == null || pedido.busca().isBlank() ? null : Textos.normalizar(pedido.busca());
        return new Filtro(pedido.moduloId(), pedido.setorId(), busca);
    }

    private CertificadoNoPainel noPainel(Emitido emitido) {
        return new CertificadoNoPainel(
                emitido.codigo(),
                CodigoDeValidacao.formatado(emitido.codigo()),
                emitido.moduloId(),
                emitido.modulo(),
                emitido.nome(),
                emitido.empresa(),
                emitido.setor(),
                data(emitido.emitidoEm()));
    }

    private LocalDate data(Instant momento) {
        return LocalDate.ofInstant(momento, relogio.getZone());
    }

    static String caminhoNoZip(Emitido certificado) {
        return seguroParaArquivo(certificado.modulo()) + "/" + seguroParaArquivo(certificado.nome()) + " - "
                + certificado.codigo() + ".pdf";
    }

    static String seguroParaArquivo(String texto) {
        String limpo = Textos.limpar(texto.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " "))
                .replaceAll("^[.\\s]+|[.\\s]+$", "");
        if (limpo.isEmpty()) {
            return "sem-nome";
        }
        return limpo.length() <= LIMITE_DO_NOME_NO_ZIP
                ? limpo
                : limpo.substring(0, LIMITE_DO_NOME_NO_ZIP).strip();
    }

    private static String alvo(Filtro filtro) {
        List<String> partes = new ArrayList<>();
        if (filtro.moduloId() != null) {
            partes.add("modulo=" + filtro.moduloId());
        }
        if (filtro.setorId() != null) {
            partes.add("setor=" + filtro.setorId());
        }
        if (filtro.busca() != null) {
            partes.add("busca=" + filtro.busca());
        }
        return partes.isEmpty() ? "todos" : String.join(", ", partes);
    }
}

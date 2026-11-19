package br.com.unimed.uniquiz.certificado.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.auditoria.dominio.CatalogoDeRotas;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificadosNoPainel;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificadosNoPainel.ExportacaoPreparada;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificadosNoPainel.ModuloComCertificado;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificadosNoPainel.PaginaDeCertificados;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDoModeloDeCertificado;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDoModeloDeCertificado.ModeloVista;
import br.com.unimed.uniquiz.certificado.dominio.PosicaoDoCampo;
import br.com.unimed.uniquiz.certificado.persistencia.ConsultasDeCertificados.Filtro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/admin/certificados")
class AdminCertificadoController {

    record PedidoDeLayout(@NotNull List<PosicaoDoCampo> campos) {}

    record PedidoDeLiberacao(@NotNull Boolean liberado) {}

    private final ServicoDeCertificadosNoPainel painel;
    private final ServicoDoModeloDeCertificado modelo;

    AdminCertificadoController(ServicoDeCertificadosNoPainel painel, ServicoDoModeloDeCertificado modelo) {
        this.painel = painel;
        this.modelo = modelo;
    }

    @GetMapping
    PaginaDeCertificados emitidos(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @RequestParam(required = false) Long moduloId,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) @Size(max = 100) String busca,
            @RequestParam(defaultValue = "0") int pagina) {
        return painel.emitidos(usuario.escopo(), new Filtro(moduloId, setorId, busca), pagina);
    }

    @GetMapping("/modulos")
    List<ModuloComCertificado> modulos(@AuthenticationPrincipal UsuarioLogado usuario) {
        return painel.modulos(usuario.escopo());
    }

    @PostMapping("/modulos/{moduloId}/liberacao")
    ResponseEntity<Void> liberar(
            @PathVariable Long moduloId, @RequestBody @Valid PedidoDeLiberacao pedido, HttpServletRequest requisicao) {
        requisicao.setAttribute(
                CatalogoDeRotas.ATRIBUTO_DO_ALVO, "moduloId=%d, liberado=%s".formatted(moduloId, pedido.liberado()));
        painel.liberar(moduloId, pedido.liberado());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exportar")
    void exportar(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @RequestParam(required = false) Long moduloId,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) @Size(max = 100) String busca,
            HttpServletResponse resposta)
            throws IOException {
        ExportacaoPreparada exportacao =
                painel.prepararExportacao(usuario.escopo(), new Filtro(moduloId, setorId, busca));
        resposta.setContentType("application/zip");
        resposta.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(exportacao.nomeDoArquivo())
                        .build()
                        .toString());
        resposta.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        resposta.setHeader("X-Accel-Buffering", "no");
        painel.exportar(usuario, exportacao, resposta.getOutputStream());
    }

    @GetMapping("/modelo")
    ModeloVista modelo() {
        return modelo.vista();
    }

    @PutMapping("/modelo")
    ModeloVista salvarModelo(
            @AuthenticationPrincipal UsuarioLogado usuario, @RequestBody @Valid PedidoDeLayout pedido) {
        return modelo.salvarLayout(pedido.campos(), usuario.id());
    }

    @PostMapping(value = "/modelo/previa", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> previa(@RequestBody @Valid PedidoDeLayout pedido) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename("previa-do-certificado.pdf")
                                .build()
                                .toString())
                .body(modelo.exemplo(pedido.campos()));
    }

    @PostMapping("/modelo/arte")
    ModeloVista enviarArte(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @RequestPart("arquivo") MultipartFile arquivo,
            HttpServletRequest requisicao)
            throws IOException {
        requisicao.setAttribute(CatalogoDeRotas.ATRIBUTO_DO_ALVO, "arte=" + nomeCurto(arquivo.getOriginalFilename()));
        return modelo.enviarArte(arquivo.getOriginalFilename(), arquivo.getBytes(), usuario.id());
    }

    @DeleteMapping("/modelo/arte")
    ModeloVista removerArte(@AuthenticationPrincipal UsuarioLogado usuario) {
        return modelo.removerArte(usuario.id());
    }

    @GetMapping(value = "/modelo/arte", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> arte() {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(modelo.previaDaArte());
    }

    private static String nomeCurto(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.length() <= 120 ? nome : nome.substring(0, 120);
    }
}

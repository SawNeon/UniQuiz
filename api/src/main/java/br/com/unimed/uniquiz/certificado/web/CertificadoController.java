package br.com.unimed.uniquiz.certificado.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificados;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificados.ArquivoPdf;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificados.CertificadoVista;
import br.com.unimed.uniquiz.certificado.aplicacao.ServicoDeCertificados.Validacao;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CertificadoController {

    private final ServicoDeCertificados certificados;

    CertificadoController(ServicoDeCertificados certificados) {
        this.certificados = certificados;
    }

    @GetMapping("/api/certificados")
    List<CertificadoVista> meus(@AuthenticationPrincipal UsuarioLogado usuario) {
        return certificados.doColaborador(usuario.id());
    }

    @PostMapping("/api/modulos/{moduloId}/certificado")
    CertificadoVista emitir(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable Long moduloId) {
        return certificados.emitir(usuario.id(), moduloId);
    }

    @GetMapping("/api/certificados/{codigo}/pdf")
    ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UsuarioLogado usuario, @PathVariable String codigo) {
        ArquivoPdf arquivo = certificados.pdf(usuario.id(), codigo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(arquivo.nome())
                                .build()
                                .toString())
                .body(arquivo.conteudo());
    }

    @GetMapping("/api/publico/certificados/{codigo}")
    Validacao validar(@PathVariable String codigo) {
        return certificados.validar(codigo);
    }
}

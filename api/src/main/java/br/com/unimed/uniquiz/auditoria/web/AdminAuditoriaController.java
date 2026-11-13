package br.com.unimed.uniquiz.auditoria.web;

import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeConsultaDeAuditoria;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeConsultaDeAuditoria.Alertas;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeConsultaDeAuditoria.OpcaoDeAcao;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeConsultaDeAuditoria.Pagina;
import br.com.unimed.uniquiz.auditoria.aplicacao.ServicoDeConsultaDeAuditoria.Pedido;
import br.com.unimed.uniquiz.auditoria.dominio.AcaoAuditada;
import br.com.unimed.uniquiz.auditoria.dominio.ResultadoDaAcao;
import br.com.unimed.uniquiz.auditoria.dominio.TipoDeAtor;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/auditoria")
class AdminAuditoriaController {

    private final ServicoDeConsultaDeAuditoria auditoria;

    AdminAuditoriaController(ServicoDeConsultaDeAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    Pagina buscar(
            @RequestParam(required = false) TipoDeAtor atorTipo,
            @RequestParam(required = false) AcaoAuditada acao,
            @RequestParam(required = false) ResultadoDaAcao resultado,
            @RequestParam(required = false) @Size(max = 100) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina) {
        return auditoria.buscar(new Pedido(atorTipo, acao, resultado, busca, de, ate, pagina));
    }

    @GetMapping("/alertas")
    Alertas alertas() {
        return auditoria.alertas();
    }

    @GetMapping("/acoes")
    List<OpcaoDeAcao> acoes() {
        return auditoria.acoes();
    }
}

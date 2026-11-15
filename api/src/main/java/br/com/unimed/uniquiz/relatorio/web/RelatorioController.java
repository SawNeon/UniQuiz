package br.com.unimed.uniquiz.relatorio.web;

import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.comum.RecursoNaoEncontrado;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeExportacao;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeExportacao.ArquivoExportado;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeExportacao.Parametros;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.Conformidade;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.Desempenho;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.Engajamento;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.Filtros;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.PaginaDePessoas;
import br.com.unimed.uniquiz.relatorio.aplicacao.ServicoDeRelatorios.PedidoDePessoas;
import br.com.unimed.uniquiz.relatorio.dominio.FormatoDeExportacao;
import br.com.unimed.uniquiz.relatorio.dominio.RelatorioExportavel;
import br.com.unimed.uniquiz.relatorio.dominio.SituacaoNoModulo;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/relatorios")
class RelatorioController {

    private final ServicoDeRelatorios relatorios;
    private final ServicoDeExportacao exportacao;

    RelatorioController(ServicoDeRelatorios relatorios, ServicoDeExportacao exportacao) {
        this.relatorios = relatorios;
        this.exportacao = exportacao;
    }

    @GetMapping("/filtros")
    Filtros filtros(@AuthenticationPrincipal UsuarioLogado usuario) {
        return relatorios.filtros(usuario.escopo());
    }

    @GetMapping("/conformidade")
    Conformidade conformidade(
            @AuthenticationPrincipal UsuarioLogado usuario, @RequestParam(required = false) Long setorId) {
        return relatorios.conformidade(usuario.escopo(), setorId);
    }

    @GetMapping("/conformidade/pessoas")
    PaginaDePessoas pessoas(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long moduloId,
            @RequestParam(required = false) SituacaoNoModulo situacao,
            @RequestParam(required = false) @Size(max = 100) String busca,
            @RequestParam(defaultValue = "0") int pagina) {
        return relatorios.pessoas(usuario.escopo(), new PedidoDePessoas(setorId, moduloId, situacao, busca), pagina);
    }

    @GetMapping("/desempenho")
    Desempenho desempenho(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long moduloId) {
        return relatorios.desempenho(usuario.escopo(), de, ate, moduloId);
    }

    @GetMapping("/engajamento")
    Engajamento engajamento(
            @AuthenticationPrincipal UsuarioLogado usuario, @RequestParam(required = false) Integer semanas) {
        return relatorios.engajamento(usuario.escopo(), semanas);
    }

    @GetMapping("/{relatorio}/exportar")
    ResponseEntity<byte[]> exportar(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @PathVariable String relatorio,
            @RequestParam(defaultValue = "CSV") FormatoDeExportacao formato,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long moduloId,
            @RequestParam(required = false) SituacaoNoModulo situacao,
            @RequestParam(required = false) @Size(max = 100) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Integer semanas) {
        RelatorioExportavel escolhido = RelatorioExportavel.doCaminho(relatorio)
                .orElseThrow(() -> new RecursoNaoEncontrado("Relatório não encontrado."));
        ArquivoExportado arquivo = exportacao.exportar(
                usuario, escolhido, formato, new Parametros(setorId, moduloId, situacao, busca, de, ate, semanas));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipoDeConteudo()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(arquivo.nome())
                                .build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(arquivo.conteudo());
    }
}

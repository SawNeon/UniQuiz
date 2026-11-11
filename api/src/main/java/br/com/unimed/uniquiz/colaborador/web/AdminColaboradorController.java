package br.com.unimed.uniquiz.colaborador.web;

import br.com.unimed.uniquiz.colaborador.aplicacao.ResumoDaImportacao;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores.ColaboradorParaAdmin;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeImportacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/colaboradores")
class AdminColaboradorController {

    record PedidoDeAjuste(@NotBlank @Size(max = 200) String nome, boolean ativo) {}

    private final ServicoDeColaboradores colaboradores;
    private final ServicoDeImportacao importacao;

    AdminColaboradorController(ServicoDeColaboradores colaboradores, ServicoDeImportacao importacao) {
        this.colaboradores = colaboradores;
        this.importacao = importacao;
    }

    @GetMapping
    List<ColaboradorParaAdmin> buscar(@RequestParam(defaultValue = "") String busca) {
        return colaboradores.buscar(busca);
    }

    @PostMapping("/importacao/previa")
    ResumoDaImportacao previa(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        return importacao.previa(arquivo.getOriginalFilename(), arquivo.getBytes());
    }

    @PostMapping("/importacao/confirmar")
    ResumoDaImportacao confirmar(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        return importacao.confirmar(arquivo.getOriginalFilename(), arquivo.getBytes());
    }

    @PatchMapping("/{colaboradorId}")
    ResponseEntity<Void> ajustar(@PathVariable Long colaboradorId, @Valid @RequestBody PedidoDeAjuste pedido) {
        colaboradores.ajustar(colaboradorId, pedido.nome(), pedido.ativo());
        return ResponseEntity.noContent().build();
    }
}

package br.com.unimed.uniquiz.conteudo.web;

import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.DadosDaLicao;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.DadosDoDesafio;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.DadosDoModulo;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.NovaOrdem;
import br.com.unimed.uniquiz.conteudo.aplicacao.ConteudoParaAdmin.NovoStatus;
import br.com.unimed.uniquiz.conteudo.aplicacao.ServicoDeConteudoAdmin;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
class AdminConteudoController {

    private final ServicoDeConteudoAdmin conteudo;

    AdminConteudoController(ServicoDeConteudoAdmin conteudo) {
        this.conteudo = conteudo;
    }

    @GetMapping("/modulos")
    List<ConteudoParaAdmin.Modulo> modulos() {
        return conteudo.listarModulos();
    }

    @PostMapping("/modulos")
    @ResponseStatus(HttpStatus.CREATED)
    ConteudoParaAdmin.Modulo criarModulo(@Valid @RequestBody DadosDoModulo dados) {
        return conteudo.criarModulo(dados);
    }

    @PutMapping("/modulos/{moduloId}")
    ConteudoParaAdmin.Modulo atualizarModulo(@PathVariable Long moduloId, @Valid @RequestBody DadosDoModulo dados) {
        return conteudo.atualizarModulo(moduloId, dados);
    }

    @PostMapping("/modulos/{moduloId}/status")
    ResponseEntity<Void> statusDoModulo(@PathVariable Long moduloId, @Valid @RequestBody NovoStatus pedido) {
        conteudo.mudarStatusDoModulo(moduloId, pedido.status());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/modulos/ordem")
    ResponseEntity<Void> ordemDosModulos(@Valid @RequestBody NovaOrdem pedido) {
        conteudo.reordenarModulos(pedido.ids());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modulos/{moduloId}/licoes")
    @ResponseStatus(HttpStatus.CREATED)
    ConteudoParaAdmin.Licao criarLicao(@PathVariable Long moduloId, @Valid @RequestBody DadosDaLicao dados) {
        return conteudo.criarLicao(moduloId, dados);
    }

    @PatchMapping("/modulos/{moduloId}/licoes/ordem")
    ResponseEntity<Void> ordemDasLicoes(@PathVariable Long moduloId, @Valid @RequestBody NovaOrdem pedido) {
        conteudo.reordenarLicoes(moduloId, pedido.ids());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/licoes/{licaoId}")
    ConteudoParaAdmin.Licao renomearLicao(@PathVariable Long licaoId, @Valid @RequestBody DadosDaLicao dados) {
        return conteudo.renomearLicao(licaoId, dados);
    }

    @PostMapping("/licoes/{licaoId}/status")
    ResponseEntity<Void> statusDaLicao(@PathVariable Long licaoId, @Valid @RequestBody NovoStatus pedido) {
        conteudo.mudarStatusDaLicao(licaoId, pedido.status());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/licoes/{licaoId}/desafios")
    List<ConteudoParaAdmin.Desafio> desafios(@PathVariable Long licaoId) {
        return conteudo.listarDesafios(licaoId);
    }

    @PostMapping("/licoes/{licaoId}/desafios")
    @ResponseStatus(HttpStatus.CREATED)
    ConteudoParaAdmin.Desafio criarDesafio(@PathVariable Long licaoId, @Valid @RequestBody DadosDoDesafio dados) {
        return conteudo.criarDesafio(licaoId, dados);
    }

    @PatchMapping("/licoes/{licaoId}/desafios/ordem")
    ResponseEntity<Void> ordemDosDesafios(@PathVariable Long licaoId, @Valid @RequestBody NovaOrdem pedido) {
        conteudo.reordenarDesafios(licaoId, pedido.ids());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/desafios/{desafioId}")
    ConteudoParaAdmin.Desafio atualizarDesafio(
            @PathVariable Long desafioId, @Valid @RequestBody DadosDoDesafio dados) {
        return conteudo.atualizarDesafio(desafioId, dados);
    }

    @PostMapping("/desafios/{desafioId}/status")
    ResponseEntity<Void> statusDoDesafio(@PathVariable Long desafioId, @Valid @RequestBody NovoStatus pedido) {
        conteudo.mudarStatusDoDesafio(desafioId, pedido.status());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/desafios/{desafioId}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    ConteudoParaAdmin.Desafio duplicar(@PathVariable Long desafioId) {
        return conteudo.duplicarDesafio(desafioId);
    }
}

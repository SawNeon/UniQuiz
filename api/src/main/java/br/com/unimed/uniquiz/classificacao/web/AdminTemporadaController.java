package br.com.unimed.uniquiz.classificacao.web;

import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDePontos;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeTemporadas;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeTemporadas.DadosDaTemporada;
import br.com.unimed.uniquiz.classificacao.aplicacao.ServicoDeTemporadas.TemporadaVista;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
class AdminTemporadaController {

    private final ServicoDeTemporadas temporadas;
    private final ServicoDePontos pontos;

    AdminTemporadaController(ServicoDeTemporadas temporadas, ServicoDePontos pontos) {
        this.temporadas = temporadas;
        this.pontos = pontos;
    }

    @GetMapping("/temporadas")
    List<TemporadaVista> listar() {
        return temporadas.listar();
    }

    @PostMapping("/temporadas")
    @ResponseStatus(HttpStatus.CREATED)
    TemporadaVista criar(@Valid @RequestBody DadosDaTemporada dados) {
        return temporadas.criar(dados);
    }

    @PutMapping("/temporadas/{temporadaId}")
    TemporadaVista atualizar(@PathVariable Long temporadaId, @Valid @RequestBody DadosDaTemporada dados) {
        return temporadas.atualizar(temporadaId, dados);
    }

    @PostMapping("/colaboradores/{colaboradorId}/zerar-pontos")
    ResponseEntity<Void> zerarPontos(@PathVariable Long colaboradorId) {
        pontos.zerarNaTemporadaAtiva(colaboradorId);
        return ResponseEntity.noContent().build();
    }
}

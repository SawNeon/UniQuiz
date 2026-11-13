package br.com.unimed.uniquiz.acesso.web;

import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel.DadosDoUsuario;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel.SenhaProvisoria;
import br.com.unimed.uniquiz.acesso.aplicacao.ServicoDeUsuariosDoPainel.UsuarioDoPainel;
import br.com.unimed.uniquiz.acesso.aplicacao.UsuarioLogado;
import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores;
import br.com.unimed.uniquiz.colaborador.aplicacao.ServicoDeColaboradores.ItemDaLista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
class AdminUsuarioController {

    record PedidoDeCriacao(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Size(max = 60) String usuario,
            @NotNull PerfilDoPainel perfil,
            Set<Long> setores) {}

    record PedidoDeEdicao(@NotBlank @Size(max = 150) String nome, @NotNull PerfilDoPainel perfil, Set<Long> setores) {}

    private final ServicoDeUsuariosDoPainel usuarios;
    private final ServicoDeColaboradores colaboradores;

    AdminUsuarioController(ServicoDeUsuariosDoPainel usuarios, ServicoDeColaboradores colaboradores) {
        this.usuarios = usuarios;
        this.colaboradores = colaboradores;
    }

    @GetMapping
    List<UsuarioDoPainel> listar() {
        return usuarios.listar();
    }

    @GetMapping("/setores")
    List<ItemDaLista> setores() {
        return colaboradores.setoresComEmpresa();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SenhaProvisoria criar(@Valid @RequestBody PedidoDeCriacao pedido) {
        return usuarios.criar(pedido.usuario(), dados(pedido.nome(), pedido.perfil(), pedido.setores()));
    }

    @PutMapping("/{usuarioId}")
    ResponseEntity<Void> editar(
            @AuthenticationPrincipal UsuarioLogado quemAltera,
            @PathVariable Long usuarioId,
            @Valid @RequestBody PedidoDeEdicao pedido) {
        usuarios.editar(usuarioId, dados(pedido.nome(), pedido.perfil(), pedido.setores()), quemAltera.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{usuarioId}/desativar")
    ResponseEntity<Void> desativar(@AuthenticationPrincipal UsuarioLogado quemAltera, @PathVariable Long usuarioId) {
        usuarios.desativar(usuarioId, quemAltera.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{usuarioId}/reativar")
    ResponseEntity<Void> reativar(@PathVariable Long usuarioId) {
        usuarios.reativar(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{usuarioId}/desbloquear")
    ResponseEntity<Void> desbloquear(@PathVariable Long usuarioId) {
        usuarios.desbloquear(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{usuarioId}/redefinir-senha")
    SenhaProvisoria redefinirSenha(@AuthenticationPrincipal UsuarioLogado quemAltera, @PathVariable Long usuarioId) {
        return usuarios.redefinirSenha(usuarioId, quemAltera.id());
    }

    @PostMapping("/{usuarioId}/encerrar-sessoes")
    ResponseEntity<Void> encerrarSessoes(@PathVariable Long usuarioId) {
        usuarios.encerrarSessoes(usuarioId);
        return ResponseEntity.noContent().build();
    }

    private static DadosDoUsuario dados(String nome, PerfilDoPainel perfil, Set<Long> setores) {
        return new DadosDoUsuario(nome, perfil, setores == null ? Set.of() : setores);
    }
}

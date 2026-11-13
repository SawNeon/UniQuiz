package br.com.unimed.uniquiz.acesso.aplicacao;

import br.com.unimed.uniquiz.acesso.dominio.EscopoDeSetores;
import br.com.unimed.uniquiz.acesso.dominio.PerfilDoPainel;
import br.com.unimed.uniquiz.acesso.dominio.Permissao;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record UsuarioLogado(
        Perfil perfil, Long id, PerfilDoPainel perfilDoPainel, EscopoDeSetores escopo, boolean precisaTrocarSenha) {

    public static final String PAPEL_DO_PAINEL = "ROLE_ADMIN";
    public static final String PAPEL_DO_COLABORADOR = "ROLE_COLABORADOR";

    public enum Perfil {
        COLABORADOR,
        ADMINISTRADOR
    }

    public static UsuarioLogado colaborador(Long id) {
        return new UsuarioLogado(Perfil.COLABORADOR, id, null, EscopoDeSetores.somente(Set.of()), false);
    }

    public static UsuarioLogado doPainel(
            Long id, PerfilDoPainel perfilDoPainel, EscopoDeSetores escopo, boolean precisaTrocarSenha) {
        return new UsuarioLogado(Perfil.ADMINISTRADOR, id, perfilDoPainel, escopo, precisaTrocarSenha);
    }

    public Set<Permissao> permissoes() {
        if (perfil == Perfil.COLABORADOR || precisaTrocarSenha) {
            return Set.of();
        }
        return perfilDoPainel.permissoes();
    }

    public List<String> autoridades() {
        if (perfil == Perfil.COLABORADOR) {
            return List.of(PAPEL_DO_COLABORADOR);
        }
        List<String> autoridades = new ArrayList<>();
        autoridades.add(PAPEL_DO_PAINEL);
        permissoes().stream().map(Permissao::autoridade).forEach(autoridades::add);
        return autoridades;
    }
}

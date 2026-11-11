package br.com.unimed.uniquiz.acesso.aplicacao;

public record UsuarioLogado(Perfil perfil, Long id) {

    public enum Perfil {
        COLABORADOR,
        ADMINISTRADOR
    }

    public String papel() {
        return perfil == Perfil.ADMINISTRADOR ? "ROLE_ADMIN" : "ROLE_COLABORADOR";
    }
}

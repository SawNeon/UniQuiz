package br.com.unimed.uniquiz.acesso.dominio;

import java.util.Set;

public record EscopoDeSetores(boolean irrestrito, Set<Long> setores) {

    private static final EscopoDeSetores TODOS = new EscopoDeSetores(true, Set.of());

    public EscopoDeSetores {
        setores = Set.copyOf(setores);
    }

    public static EscopoDeSetores todos() {
        return TODOS;
    }

    public static EscopoDeSetores somente(Set<Long> setores) {
        return new EscopoDeSetores(false, setores);
    }

    public static EscopoDeSetores para(PerfilDoPainel perfil, Set<Long> setoresAtribuidos) {
        return perfil.limitadoAosSetores() ? somente(setoresAtribuidos) : todos();
    }

    public boolean permite(Long setorId) {
        return irrestrito || setores.contains(setorId);
    }
}

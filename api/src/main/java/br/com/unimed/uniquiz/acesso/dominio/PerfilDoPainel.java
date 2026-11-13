package br.com.unimed.uniquiz.acesso.dominio;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum PerfilDoPainel {
    ADMINISTRADOR(EnumSet.allOf(Permissao.class), false),
    GESTOR(EnumSet.of(Permissao.VER_RELATORIOS, Permissao.EXPORTAR_RELATORIOS), true),
    AUDITOR(EnumSet.of(Permissao.VER_RELATORIOS, Permissao.EXPORTAR_RELATORIOS, Permissao.VER_AUDITORIA), false);

    private final Set<Permissao> permissoes;
    private final boolean limitadoAosSetores;

    PerfilDoPainel(Set<Permissao> permissoes, boolean limitadoAosSetores) {
        this.permissoes = Collections.unmodifiableSet(permissoes);
        this.limitadoAosSetores = limitadoAosSetores;
    }

    public Set<Permissao> permissoes() {
        return permissoes;
    }

    public boolean limitadoAosSetores() {
        return limitadoAosSetores;
    }
}

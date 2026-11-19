package br.com.unimed.uniquiz.auditoria.dominio;

public enum AcaoAuditada {
    MODULO_CRIADO("Criou um módulo"),
    MODULO_EDITADO("Editou um módulo"),
    MODULO_SITUACAO_ALTERADA("Mudou a situação de um módulo"),
    MODULOS_REORDENADOS("Reordenou os módulos"),
    LICAO_CRIADA("Criou uma lição"),
    LICAO_EDITADA("Editou uma lição"),
    LICAO_SITUACAO_ALTERADA("Mudou a situação de uma lição"),
    LICOES_REORDENADAS("Reordenou as lições"),
    DESAFIO_CRIADO("Criou um desafio"),
    DESAFIO_EDITADO("Editou um desafio"),
    DESAFIO_SITUACAO_ALTERADA("Mudou a situação de um desafio"),
    DESAFIO_DUPLICADO("Duplicou um desafio"),
    DESAFIOS_REORDENADOS("Reordenou os desafios"),
    COLABORADOR_CADASTRADO("Cadastrou um colaborador"),
    COLABORADOR_AJUSTADO("Ajustou um colaborador"),
    PIN_REDEFINIDO("Redefiniu o PIN de um colaborador"),
    PLANILHA_IMPORTADA("Importou a planilha do RH"),
    PONTOS_ZERADOS("Zerou os pontos de um colaborador"),
    TEMPORADA_CRIADA("Criou uma temporada"),
    TEMPORADA_EDITADA("Editou uma temporada"),
    USUARIO_DO_PAINEL_CRIADO("Criou um usuário do painel"),
    USUARIO_DO_PAINEL_EDITADO("Editou um usuário do painel"),
    USUARIO_DO_PAINEL_DESATIVADO("Desativou um usuário do painel"),
    USUARIO_DO_PAINEL_REATIVADO("Reativou um usuário do painel"),
    USUARIO_DO_PAINEL_DESBLOQUEADO("Desbloqueou um usuário do painel"),
    SENHA_REDEFINIDA("Redefiniu a senha de um usuário do painel"),
    SESSOES_DE_USUARIO_ENCERRADAS("Encerrou as sessões de um usuário do painel"),
    SENHA_TROCADA("Trocou a própria senha"),
    SESSAO_ENCERRADA("Encerrou uma das próprias sessões"),
    OUTRAS_SESSOES_ENCERRADAS("Saiu dos outros aparelhos"),
    ENTRADA_NO_PAINEL("Entrou no painel"),
    SAIDA_DO_PAINEL("Saiu do painel"),
    ENTRADA_DO_COLABORADOR("Entrou no app"),
    SAIDA_DO_COLABORADOR("Saiu do app"),
    RELATORIO_EXPORTADO("Exportou um relatório"),
    MODELO_DE_CERTIFICADO_EDITADO("Editou a posição dos campos do certificado"),
    ARTE_DO_CERTIFICADO_ENVIADA("Enviou a arte do certificado"),
    ARTE_DO_CERTIFICADO_REMOVIDA("Removeu a arte do certificado"),
    LIBERACAO_DO_CERTIFICADO_ALTERADA("Liberou ou travou o certificado de um módulo"),
    CERTIFICADOS_EXPORTADOS("Exportou certificados"),
    ACAO_NAO_CATALOGADA("Ação do painel sem descrição");

    private final String descricao;

    AcaoAuditada(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}

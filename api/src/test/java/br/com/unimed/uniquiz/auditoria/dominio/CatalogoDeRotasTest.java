package br.com.unimed.uniquiz.auditoria.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogoDeRotasTest {

    @Test
    void traduzARotaModeloNaAcao() {
        assertThat(CatalogoDeRotas.acaoDa("POST", "/api/admin/usuarios/{usuarioId}/desativar"))
                .isEqualTo(AcaoAuditada.USUARIO_DO_PAINEL_DESATIVADO);
        assertThat(CatalogoDeRotas.conhecida("PUT", "/api/admin/temporadas/{temporadaId}"))
                .isTrue();
    }

    @Test
    void rotaForaDoCatalogoViraAcaoNaoCatalogada() {
        assertThat(CatalogoDeRotas.acaoDa("POST", "/api/admin/rota-nova")).isEqualTo(AcaoAuditada.ACAO_NAO_CATALOGADA);
        assertThat(CatalogoDeRotas.conhecida("POST", "/api/admin/rota-nova")).isFalse();
    }

    @Test
    void loginESaidaFicamComOProprioServico() {
        assertThat(CatalogoDeRotas.registradaPeloInterceptador("POST", "/api/admin/login"))
                .isFalse();
        assertThat(CatalogoDeRotas.conhecida("POST", "/api/admin/login")).isTrue();
        assertThat(CatalogoDeRotas.registradaPeloInterceptador("POST", "/api/admin/modulos"))
                .isTrue();
    }

    @Test
    void alvoListaAsVariaveisDaRotaEmOrdem() {
        assertThat(CatalogoDeRotas.alvoDe(Map.of("moduloId", "4", "licaoId", "9")))
                .isEqualTo("licaoId=9, moduloId=4");
        assertThat(CatalogoDeRotas.alvoDe(Map.of())).isNull();
        assertThat(CatalogoDeRotas.alvoDe(null)).isNull();
    }

    @Test
    void ocorrenciaCortaTextosLongosNoLimiteDaTabela() {
        OcorrenciaDeAuditoria ocorrencia = OcorrenciaDeAuditoria.de(
                        TipoDeAtor.ADMINISTRADOR, 1L, AcaoAuditada.MODULO_CRIADO)
                .descrevendoOAtor("a".repeat(250))
                .comAlvo("b".repeat(400))
                .comFalha("c".repeat(600));

        assertThat(ocorrencia.atorDescricao()).hasSize(200);
        assertThat(ocorrencia.alvo()).hasSize(300);
        assertThat(ocorrencia.detalhe()).hasSize(500);
        assertThat(ocorrencia.resultado()).isEqualTo(ResultadoDaAcao.FALHA);
    }
}

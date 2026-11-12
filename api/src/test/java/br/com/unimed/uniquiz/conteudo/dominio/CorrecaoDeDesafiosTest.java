package br.com.unimed.uniquiz.conteudo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoCompletarClicando.Lacuna;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoCompletarClicando.PalavraDoBanco;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoMultiplaEscolha.Alternativa;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CorrecaoDeDesafiosTest {

    @Nested
    class MultiplaEscolha {

        private final ConteudoMultiplaEscolha desafio = new ConteudoMultiplaEscolha(List.of(
                new Alternativa("a", "Tirar dados pessoais", true, ""),
                new Alternativa("b", "Corrigir a digitação", false, "Digitação não expõe ninguém.")));

        @Test
        void acertoDevolveARespostaCerta() {
            Correcao correcao = desafio.corrigir(new RespostaDoColaborador.MultiplaEscolha("a"));

            assertThat(correcao.correta()).isTrue();
            assertThat(correcao.motivoDoErro()).isNull();
            assertThat(correcao.respostaCerta()).isEqualTo("Tirar dados pessoais");
        }

        @Test
        void erroDevolveOMotivoDaAlternativaEscolhida() {
            Correcao correcao = desafio.corrigir(new RespostaDoColaborador.MultiplaEscolha("b"));

            assertThat(correcao.correta()).isFalse();
            assertThat(correcao.motivoDoErro()).isEqualTo("Digitação não expõe ninguém.");
        }

        @Test
        void versaoDoColaboradorNaoTemGabarito() {
            ConteudoPublico publico = desafio.paraColaborador(new Random(1));

            assertThat(publico).isInstanceOf(ConteudoPublico.MultiplaEscolha.class);
            assertThat(((ConteudoPublico.MultiplaEscolha) publico).alternativas())
                    .extracting(ConteudoPublico.AlternativaPublica::texto)
                    .containsExactlyInAnyOrder("Tirar dados pessoais", "Corrigir a digitação");
        }

        @Test
        void alternativasMudamDeLugarACadaTentativaParaACertaNaoTerPosicaoFixa() {
            ConteudoMultiplaEscolha comQuatro = new ConteudoMultiplaEscolha(List.of(
                    new Alternativa("a", "Certa", true, ""),
                    new Alternativa("b", "Errada 1", false, "Motivo 1"),
                    new Alternativa("c", "Errada 2", false, "Motivo 2"),
                    new Alternativa("d", "Errada 3", false, "Motivo 3")));

            java.util.Set<String> posicoesDaCerta = new java.util.HashSet<>();
            java.util.Set<List<String>> ordens = new java.util.HashSet<>();
            for (long semente = 0; semente < 40; semente++) {
                var publico = (ConteudoPublico.MultiplaEscolha) comQuatro.paraColaborador(new Random(semente));
                List<String> ids = publico.alternativas().stream()
                        .map(ConteudoPublico.AlternativaPublica::id)
                        .toList();
                posicoesDaCerta.add(String.valueOf(ids.indexOf("a")));
                ordens.add(ids);
                assertThat(ids).containsExactlyInAnyOrder("a", "b", "c", "d");
            }

            assertThat(posicoesDaCerta).hasSize(4);
            assertThat(ordens.size()).isGreaterThan(10);
        }

        @Test
        void exigeMotivoNasAlternativasErradas() {
            ConteudoMultiplaEscolha semMotivo = new ConteudoMultiplaEscolha(
                    List.of(new Alternativa("a", "Certa", true, ""), new Alternativa("b", "Errada", false, " ")));

            assertThatThrownBy(semMotivo::validar)
                    .isInstanceOf(ErroDeNegocio.class)
                    .hasMessageContaining("Errada");
        }

        @Test
        void recusaRespostaDeOutroTipo() {
            assertThatThrownBy(() -> desafio.corrigir(new RespostaDoColaborador.VerdadeiroOuFalso(true)))
                    .isInstanceOf(ErroDeNegocio.class);
        }
    }

    @Nested
    class VerdadeiroOuFalso {

        private final ConteudoVerdadeiroOuFalso desafio =
                new ConteudoVerdadeiroOuFalso("Dados de saúde são sensíveis.", true, "A LGPD diz que são.");

        @Test
        void corrigeOsDoisLados() {
            assertThat(desafio.corrigir(new RespostaDoColaborador.VerdadeiroOuFalso(true))
                            .correta())
                    .isTrue();
            Correcao erro = desafio.corrigir(new RespostaDoColaborador.VerdadeiroOuFalso(false));
            assertThat(erro.correta()).isFalse();
            assertThat(erro.respostaCerta()).isEqualTo("Verdadeiro");
        }
    }

    @Nested
    class CompletarClicando {

        private final ConteudoCompletarClicando desafio = new ConteudoCompletarClicando(
                "Use apenas ferramentas {{1}} pela TI.",
                List.of(new Lacuna(
                        1,
                        List.of(
                                new PalavraDoBanco("aprovadas", true, ""),
                                new PalavraDoBanco("gratuitas", false, "Gratuita não quer dizer segura.")))));

        @Test
        void acertoMontaAFraseCompleta() {
            Correcao correcao = desafio.corrigir(new RespostaDoColaborador.CompletarClicando(Map.of(1, "aprovadas")));

            assertThat(correcao.correta()).isTrue();
            assertThat(correcao.respostaCerta()).isEqualTo("Use apenas ferramentas aprovadas pela TI.");
        }

        @Test
        void erroUsaOMotivoDaPalavraEscolhida() {
            Correcao correcao = desafio.corrigir(new RespostaDoColaborador.CompletarClicando(Map.of(1, "gratuitas")));

            assertThat(correcao.correta()).isFalse();
            assertThat(correcao.motivoDoErro()).isEqualTo("Gratuita não quer dizer segura.");
        }

        @Test
        void exigeTodasAsLacunasPreenchidas() {
            assertThatThrownBy(() -> desafio.corrigir(new RespostaDoColaborador.CompletarClicando(Map.of())))
                    .isInstanceOf(ErroDeNegocio.class);
        }

        @Test
        void validaQueCadaMarcadorTemBanco() {
            ConteudoCompletarClicando semBanco =
                    new ConteudoCompletarClicando("Frase com {{1}} e {{2}}.", desafio.lacunas());

            assertThatThrownBy(semBanco::validar).isInstanceOf(ErroDeNegocio.class);
        }

        @Test
        void versaoDoColaboradorTemAsPalavrasSemIndicarACerta() {
            var publico = (ConteudoPublico.CompletarClicando) desafio.paraColaborador(new Random(7));

            assertThat(publico.texto()).contains("{{1}}");
            assertThat(publico.lacunas().getFirst().palavras()).containsExactlyInAnyOrder("aprovadas", "gratuitas");
        }
    }
}

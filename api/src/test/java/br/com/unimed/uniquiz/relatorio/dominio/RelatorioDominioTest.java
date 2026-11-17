package br.com.unimed.uniquiz.relatorio.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.unimed.uniquiz.conteudo.dominio.ConteudoCompletarClicando;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoMultiplaEscolha;
import br.com.unimed.uniquiz.conteudo.dominio.ConteudoVerdadeiroOuFalso;
import br.com.unimed.uniquiz.conteudo.dominio.Dificuldade;
import br.com.unimed.uniquiz.conteudo.dominio.RespostaDoColaborador;
import br.com.unimed.uniquiz.relatorio.dominio.CalibracaoDaDificuldade.Avaliacao;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RelatorioDominioTest {

    @Nested
    class Csv {

        @ParameterizedTest
        @ValueSource(strings = {"=1+1", "+55", "-2", "@SOMA(A1)", "\tcmd", "\rcmd"})
        void neutralizaTextoQueOExcelLeriaComoFormula(String perigoso) {
            assertThat(EscritaEmCsv.protegerContraFormula(perigoso)).isEqualTo("'" + perigoso);
        }

        @Test
        void naoMexeEmTextoComum() {
            assertThat(EscritaEmCsv.protegerContraFormula("Ana Exemplo")).isEqualTo("Ana Exemplo");
            assertThat(EscritaEmCsv.protegerContraFormula("")).isEmpty();
        }

        @Test
        void numerosDatasEVaziosNaoSaoTratadosComoTexto() {
            assertThat(EscritaEmCsv.celula(-3)).isEqualTo("-3");
            assertThat(EscritaEmCsv.celula(66.66)).isEqualTo("66,7");
            assertThat(EscritaEmCsv.celula(LocalDate.of(2026, 12, 7))).isEqualTo("07/12/2026");
            assertThat(EscritaEmCsv.celula(null)).isEmpty();
        }

        @Test
        void colocaEntreAspasQuandoOTextoTemSeparadorAspasOuQuebra() {
            assertThat(EscritaEmCsv.celula("a;b")).isEqualTo("\"a;b\"");
            assertThat(EscritaEmCsv.celula("diz \"oi\"")).isEqualTo("\"diz \"\"oi\"\"\"");
            assertThat(EscritaEmCsv.celula("linha\nnova")).isEqualTo("\"linha\nnova\"");
        }

        @Test
        void arquivoComecaComMarcaUtf8EUsaPontoEVirgula() {
            Planilha planilha = new Planilha(
                    "Teste", List.of("Nome", "Total"), List.of(Arrays.asList("João", 2), Arrays.asList(null, 1.25)));

            String texto = new String(EscritaEmCsv.escrever(planilha), StandardCharsets.UTF_8);

            assertThat(texto).isEqualTo("﻿Nome;Total\r\nJoão;2\r\n;1,3\r\n");
        }
    }

    @Nested
    class Calibracao {

        @ParameterizedTest
        @CsvSource({
            "FACIL, 9, 9, , POUCOS_DADOS",
            "FACIL, 10, 9, FACIL, ADEQUADA",
            "DIFICIL, 10, 9, FACIL, MAIS_FACIL_QUE_O_PREVISTO",
            "MEDIO, 10, 5, MEDIO, ADEQUADA",
            "FACIL, 10, 7, MEDIO, MAIS_DIFICIL_QUE_O_PREVISTO",
            "MEDIO, 20, 4, DIFICIL, MAIS_DIFICIL_QUE_O_PREVISTO",
            "DIFICIL, 10, 2, DIFICIL, ADEQUADA"
        })
        void comparaOAcertoRealComONivelDeclarado(
                Dificuldade declarada, long respostas, long acertos, Dificuldade observada, Avaliacao avaliacao) {
            assertThat(CalibracaoDaDificuldade.calibrar(declarada, respostas, acertos))
                    .isEqualTo(new CalibracaoDaDificuldade(observada, avaliacao));
        }
    }

    @Nested
    class Situacao {

        @ParameterizedTest
        @CsvSource({"0, 3, NAO_COMECOU", "1, 3, EM_ANDAMENTO", "3, 3, CONCLUIU", "4, 3, CONCLUIU"})
        void situacaoPeloNumeroDeLicoes(long concluidas, long total, SituacaoNoModulo esperada) {
            assertThat(SituacaoNoModulo.de(concluidas, total)).isEqualTo(esperada);
        }

        @Test
        void rotulosEmPortugues() {
            assertThat(SituacaoNoModulo.NAO_COMECOU.rotulo()).isEqualTo("Não começou");
        }
    }

    @Nested
    class SemanasDoEngajamento {

        @Test
        void semanaComecaNaSegunda() {
            assertThat(Semanas.inicioDaSemana(LocalDate.of(2026, 11, 15))).isEqualTo(LocalDate.of(2026, 11, 9));
            assertThat(Semanas.inicioDaSemana(LocalDate.of(2026, 11, 9))).isEqualTo(LocalDate.of(2026, 11, 9));
        }

        @Test
        void ultimasSemanasEmOrdemCronologica() {
            assertThat(Semanas.ultimas(LocalDate.of(2026, 11, 15), 3))
                    .containsExactly(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9));
        }
    }

    @Nested
    class Descricao {

        @Test
        void descreveCadaTipoDeResposta() {
            var multipla = new ConteudoMultiplaEscolha(List.of(
                    new ConteudoMultiplaEscolha.Alternativa("a", "Certa", true, ""),
                    new ConteudoMultiplaEscolha.Alternativa("b", "Errada", false, "x")));
            var verdadeiroOuFalso = new ConteudoVerdadeiroOuFalso("Afirmação", true, "x");
            var completar = new ConteudoCompletarClicando("{{2}} e {{1}}", List.of());

            assertThat(DescricaoDaResposta.descrever(multipla, new RespostaDoColaborador.MultiplaEscolha("b")))
                    .isEqualTo("Errada");
            assertThat(DescricaoDaResposta.descrever(multipla, new RespostaDoColaborador.MultiplaEscolha("z")))
                    .isEqualTo("Alternativa removida");
            assertThat(DescricaoDaResposta.descrever(
                            verdadeiroOuFalso, new RespostaDoColaborador.VerdadeiroOuFalso(false)))
                    .isEqualTo("Falso");
            assertThat(DescricaoDaResposta.descrever(
                            completar, new RespostaDoColaborador.CompletarClicando(Map.of(2, "dois", 1, "um"))))
                    .isEqualTo("um / dois");
            assertThat(DescricaoDaResposta.descrever(completar, new RespostaDoColaborador.CompletarClicando(Map.of())))
                    .isEqualTo("Sem palavras");
            assertThat(DescricaoDaResposta.descrever(multipla, new RespostaDoColaborador.VerdadeiroOuFalso(true)))
                    .isEqualTo("Resposta em formato antigo");
        }
    }

    @Nested
    class Texto {

        @Test
        void mostraOQueFoiPerguntadoEmVezDoEnunciadoGenerico() {
            var verdadeiroOuFalso = new ConteudoVerdadeiroOuFalso(" O CPF é um dado pessoal. ", true, "x");
            var completar = new ConteudoCompletarClicando("A {{1}} protege {{2}}.", List.of());
            var multipla = new ConteudoMultiplaEscolha(List.of());

            assertThat(TextoDoDesafio.descrever("Verdadeiro ou falso?", verdadeiroOuFalso))
                    .isEqualTo("O CPF é um dado pessoal.");
            assertThat(TextoDoDesafio.descrever("Complete a frase.", completar)).isEqualTo("A ___ protege ___.");
            assertThat(TextoDoDesafio.descrever("O que a LGPD protege?", multipla))
                    .isEqualTo("O que a LGPD protege?");
            assertThat(TextoDoDesafio.descrever("Verdadeiro ou falso?", new ConteudoVerdadeiroOuFalso(" ", true, "x")))
                    .isEqualTo("Verdadeiro ou falso?");
            assertThat(TextoDoDesafio.descrever("Complete a frase.", new ConteudoCompletarClicando(null, List.of())))
                    .isEqualTo("Complete a frase.");
        }
    }

    @Nested
    class Relatorios {

        @Test
        void encontraORelatorioPeloCaminho() {
            assertThat(RelatorioExportavel.doCaminho("engajamento")).contains(RelatorioExportavel.ENGAJAMENTO);
            assertThat(RelatorioExportavel.doCaminho("outro")).isEmpty();
            assertThat(FormatoDeExportacao.XLSX.extensao()).isEqualTo("xlsx");
        }
    }
}

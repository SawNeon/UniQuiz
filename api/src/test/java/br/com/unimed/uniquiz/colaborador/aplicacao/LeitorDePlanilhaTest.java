package br.com.unimed.uniquiz.colaborador.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.unimed.uniquiz.colaborador.dominio.LinhaDaPlanilha;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class LeitorDePlanilhaTest {

    private final LeitorDePlanilha leitor = new LeitorDePlanilha();

    @Test
    void lerCsvComVirgula() {
        List<LinhaDaPlanilha> linhas = leitor.ler(
                "rh.csv",
                "nome,setor,empresa\nMaria de Souza,Faturamento,Unimed Varginha\n".getBytes(StandardCharsets.UTF_8));

        assertThat(linhas).containsExactly(new LinhaDaPlanilha(2, "Maria de Souza", "Faturamento", "Unimed Varginha"));
    }

    @Test
    void lerCsvDoExcelComPontoEVirgulaBomEColunasForaDeOrdem() {
        String conteudo = "﻿Empresa;Nome;Setor\r\nUnimed Varginha;João Pereira;Tecnologia da Informação\r\n";

        List<LinhaDaPlanilha> linhas = leitor.ler("RH.CSV", conteudo.getBytes(StandardCharsets.UTF_8));

        assertThat(linhas)
                .containsExactly(new LinhaDaPlanilha(2, "João Pereira", "Tecnologia da Informação", "Unimed Varginha"));
    }

    @Test
    void lerXlsx() throws IOException {
        byte[] planilha;
        try (XSSFWorkbook pasta = new XSSFWorkbook();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet aba = pasta.createSheet("RH");
            preencher(aba.createRow(0), "nome", "setor", "empresa");
            preencher(aba.createRow(1), "Ana Lima", "Laboratório", "Unimed Varginha");
            aba.createRow(3).createCell(0).setCellValue("Rui Sem Setor");
            pasta.write(saida);
            planilha = saida.toByteArray();
        }

        List<LinhaDaPlanilha> linhas = leitor.ler("rh.xlsx", planilha);

        assertThat(linhas)
                .containsExactly(
                        new LinhaDaPlanilha(2, "Ana Lima", "Laboratório", "Unimed Varginha"),
                        new LinhaDaPlanilha(4, "Rui Sem Setor", "", ""));
    }

    @Test
    void recusaExtensaoDesconhecidaCabecalhoErradoEArquivoVazio() {
        byte[] qualquer = "nome,setor,empresa\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> leitor.ler("rh.txt", qualquer)).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> leitor.ler(null, qualquer)).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> leitor.ler("rh.csv", "nome,cargo\nAna,Analista\n".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("colunas");
        assertThatThrownBy(() -> leitor.ler("rh.csv", new byte[0])).isInstanceOf(ErroDeNegocio.class);
        assertThatThrownBy(() -> leitor.ler("rh.xlsx", "não é planilha".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ErroDeNegocio.class)
                .hasMessageContaining("xlsx");
    }

    private static void preencher(Row linha, String... valores) {
        for (int coluna = 0; coluna < valores.length; coluna++) {
            linha.createCell(coluna).setCellValue(valores[coluna]);
        }
    }
}

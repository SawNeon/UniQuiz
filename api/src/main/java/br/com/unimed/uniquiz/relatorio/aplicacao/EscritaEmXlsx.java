package br.com.unimed.uniquiz.relatorio.aplicacao;

import br.com.unimed.uniquiz.relatorio.dominio.Planilha;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class EscritaEmXlsx {

    private static final int LARGURA_DA_COLUNA = 22 * 256;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private EscritaEmXlsx() {}

    static byte[] escrever(Planilha planilha) {
        try (XSSFWorkbook pasta = new XSSFWorkbook();
                ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet aba = pasta.createSheet(planilha.nome());
            CellStyle cabecalho = pasta.createCellStyle();
            Font negrito = pasta.createFont();
            negrito.setBold(true);
            cabecalho.setFont(negrito);

            Row primeira = aba.createRow(0);
            for (int coluna = 0; coluna < planilha.colunas().size(); coluna++) {
                Cell celula = primeira.createCell(coluna);
                celula.setCellValue(planilha.colunas().get(coluna));
                celula.setCellStyle(cabecalho);
                aba.setColumnWidth(coluna, LARGURA_DA_COLUNA);
            }
            aba.createFreezePane(0, 1);

            for (int numero = 0; numero < planilha.linhas().size(); numero++) {
                Row linha = aba.createRow(numero + 1);
                List<Object> valores = planilha.linhas().get(numero);
                for (int coluna = 0; coluna < valores.size(); coluna++) {
                    preencher(linha.createCell(coluna), valores.get(coluna));
                }
            }
            pasta.write(saida);
            return saida.toByteArray();
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível gerar a planilha", erro);
        }
    }

    private static void preencher(Cell celula, Object valor) {
        if (valor == null) {
            celula.setBlank();
        } else if (valor instanceof Number numero) {
            celula.setCellValue(numero.doubleValue());
        } else if (valor instanceof LocalDate data) {
            celula.setCellValue(DATA.format(data));
        } else {
            celula.setCellValue(valor.toString());
        }
    }
}

package br.com.unimed.uniquiz.colaborador.aplicacao;

import br.com.unimed.uniquiz.colaborador.dominio.LinhaDaPlanilha;
import br.com.unimed.uniquiz.comum.ErroDeNegocio;
import br.com.unimed.uniquiz.comum.Textos;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class LeitorDePlanilha {

    private record LinhaBruta(int numero, List<String> celulas) {}

    public List<LinhaDaPlanilha> ler(String nomeDoArquivo, byte[] conteudo) {
        String nome = nomeDoArquivo == null ? "" : nomeDoArquivo.toLowerCase(Locale.ROOT);
        if (nome.endsWith(".xlsx")) {
            return converter(lerXlsx(conteudo));
        }
        if (nome.endsWith(".csv")) {
            return converter(lerCsv(conteudo));
        }
        throw new ErroDeNegocio("Envie a planilha em .xlsx ou .csv.");
    }

    private List<LinhaBruta> lerXlsx(byte[] conteudo) {
        try (Workbook planilha = new XSSFWorkbook(new ByteArrayInputStream(conteudo))) {
            Sheet aba = planilha.getSheetAt(0);
            DataFormatter formatador = new DataFormatter(Locale.forLanguageTag("pt-BR"));
            List<LinhaBruta> linhas = new ArrayList<>();
            for (Row linha : aba) {
                List<String> celulas = new ArrayList<>();
                for (int coluna = 0; coluna < Math.max(linha.getLastCellNum(), 0); coluna++) {
                    celulas.add(formatador.formatCellValue(linha.getCell(coluna)));
                }
                linhas.add(new LinhaBruta(linha.getRowNum() + 1, celulas));
            }
            return linhas;
        } catch (IOException | RuntimeException erro) {
            throw new ErroDeNegocio("Não foi possível ler a planilha. Confira se o arquivo é um .xlsx válido.");
        }
    }

    private List<LinhaBruta> lerCsv(byte[] conteudo) {
        String texto = new String(conteudo, StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        CSVFormat formato = CSVFormat.DEFAULT
                .builder()
                .setDelimiter(detectarDelimitador(texto))
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (CSVParser leitor = CSVParser.parse(texto, formato)) {
            List<LinhaBruta> linhas = new ArrayList<>();
            for (CSVRecord registro : leitor) {
                linhas.add(new LinhaBruta((int) registro.getRecordNumber(), registro.toList()));
            }
            return linhas;
        } catch (IOException | RuntimeException erro) {
            throw new ErroDeNegocio("Não foi possível ler a planilha. Confira se o arquivo é um .csv válido.");
        }
    }

    private static char detectarDelimitador(String texto) {
        int fimDaPrimeiraLinha = texto.indexOf('\n');
        String cabecalho = fimDaPrimeiraLinha < 0 ? texto : texto.substring(0, fimDaPrimeiraLinha);
        long pontoEVirgula = cabecalho.chars().filter(c -> c == ';').count();
        long virgula = cabecalho.chars().filter(c -> c == ',').count();
        return pontoEVirgula > virgula ? ';' : ',';
    }

    private static List<LinhaDaPlanilha> converter(List<LinhaBruta> linhas) {
        if (linhas.isEmpty()) {
            throw new ErroDeNegocio("A planilha está vazia.");
        }
        List<String> cabecalho =
                linhas.getFirst().celulas().stream().map(Textos::normalizar).toList();
        int colunaNome = cabecalho.indexOf("NOME");
        int colunaSetor = cabecalho.indexOf("SETOR");
        int colunaEmpresa = cabecalho.indexOf("EMPRESA");
        if (colunaNome < 0 || colunaSetor < 0 || colunaEmpresa < 0) {
            throw new ErroDeNegocio("A primeira linha precisa ter as colunas nome, setor e empresa.");
        }
        return linhas.stream()
                .skip(1)
                .map(linha -> new LinhaDaPlanilha(
                        linha.numero(),
                        celula(linha, colunaNome),
                        celula(linha, colunaSetor),
                        celula(linha, colunaEmpresa)))
                .toList();
    }

    private static String celula(LinhaBruta linha, int coluna) {
        return coluna < linha.celulas().size() ? linha.celulas().get(coluna) : "";
    }
}

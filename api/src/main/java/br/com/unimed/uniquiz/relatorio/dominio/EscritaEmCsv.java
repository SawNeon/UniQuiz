package br.com.unimed.uniquiz.relatorio.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class EscritaEmCsv {

    private static final String MARCA_DE_UTF8 = "﻿";
    private static final String SEPARADOR = ";";
    private static final String FIM_DE_LINHA = "\r\n";
    private static final String INICIOS_QUE_VIRAM_FORMULA = "=+-@\t\r";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private EscritaEmCsv() {}

    public static byte[] escrever(Planilha planilha) {
        StringBuilder texto = new StringBuilder(MARCA_DE_UTF8);
        texto.append(linha(List.copyOf(planilha.colunas()))).append(FIM_DE_LINHA);
        for (List<Object> valores : planilha.linhas()) {
            texto.append(linha(valores)).append(FIM_DE_LINHA);
        }
        return texto.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String celula(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Number numero) {
            return numeroComVirgula(numero);
        }
        if (valor instanceof LocalDate data) {
            return DATA.format(data);
        }
        return entreAspasSePreciso(protegerContraFormula(valor.toString()));
    }

    public static String protegerContraFormula(String texto) {
        if (!texto.isEmpty() && INICIOS_QUE_VIRAM_FORMULA.indexOf(texto.charAt(0)) >= 0) {
            return "'" + texto;
        }
        return texto;
    }

    private static String linha(List<Object> valores) {
        return valores.stream().map(EscritaEmCsv::celula).collect(Collectors.joining(SEPARADOR));
    }

    private static String entreAspasSePreciso(String texto) {
        if (texto.contains(SEPARADOR) || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    private static String numeroComVirgula(Number numero) {
        if (numero instanceof Double || numero instanceof Float || numero instanceof BigDecimal) {
            return new BigDecimal(numero.toString())
                    .setScale(1, RoundingMode.HALF_UP)
                    .toPlainString()
                    .replace('.', ',');
        }
        return numero.toString();
    }
}

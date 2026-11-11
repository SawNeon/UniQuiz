package br.com.unimed.uniquiz.jogo.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record SequenciaDeDias(int diasAtuais, int maiorSequencia, LocalDate ultimoDia) {

    public static SequenciaDeDias primeiroDia(LocalDate hoje) {
        return new SequenciaDeDias(1, 1, hoje);
    }

    public SequenciaDeDias registrar(LocalDate hoje) {
        if (!hoje.isAfter(ultimoDia)) {
            return this;
        }
        int dias = continuaEm(hoje) ? diasAtuais + 1 : 1;
        return new SequenciaDeDias(dias, Math.max(maiorSequencia, dias), hoje);
    }

    public int diasVigentes(LocalDate hoje) {
        return hoje.equals(ultimoDia) || continuaEm(hoje) ? diasAtuais : 0;
    }

    private boolean continuaEm(LocalDate dia) {
        return dia.equals(proximoDiaUtil(ultimoDia));
    }

    private static LocalDate proximoDiaUtil(LocalDate dia) {
        LocalDate proximo = dia.plusDays(1);
        while (proximo.getDayOfWeek() == DayOfWeek.SATURDAY || proximo.getDayOfWeek() == DayOfWeek.SUNDAY) {
            proximo = proximo.plusDays(1);
        }
        return proximo;
    }
}

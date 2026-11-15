package br.com.unimed.uniquiz.relatorio.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class Semanas {

    private Semanas() {}

    public static LocalDate inicioDaSemana(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static List<LocalDate> ultimas(LocalDate hoje, int quantidade) {
        LocalDate atual = inicioDaSemana(hoje);
        List<LocalDate> inicios = new ArrayList<>();
        for (int semana = quantidade - 1; semana >= 0; semana--) {
            inicios.add(atual.minusWeeks(semana));
        }
        return inicios;
    }
}

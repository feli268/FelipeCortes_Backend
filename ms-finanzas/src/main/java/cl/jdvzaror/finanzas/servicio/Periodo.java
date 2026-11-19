package cl.jdvzaror.finanzas.servicio;

import cl.jdvzaror.finanzas.seguridad.ApiException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Rango de fechas de un periodo "YYYY-MM"; sin periodo significa todo el historial. */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public static Periodo de(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            return new Periodo(LocalDate.of(1900, 1, 1), LocalDate.of(2999, 12, 31));
        }
        try {
            YearMonth mes = YearMonth.parse(periodo.trim());
            return new Periodo(mes.atDay(1), mes.atEndOfMonth());
        } catch (DateTimeException e) {
            throw ApiException.invalida("El periodo debe tener el formato AAAA-MM, por ejemplo 2026-11");
        }
    }
}

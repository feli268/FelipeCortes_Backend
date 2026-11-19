package cl.jdvzaror.finanzas.dto;

/** Resumen visible para los vecinos: solo totales, sin movimientos ni datos personales. */
public record ResumenDto(String periodo, long ingresos, long gastos, long saldo) {
}

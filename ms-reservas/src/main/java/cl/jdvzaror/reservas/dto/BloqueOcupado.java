package cl.jdvzaror.reservas.dto;

import java.time.LocalTime;

/** Horario ocupado de una dependencia; no incluye datos del solicitante. */
public record BloqueOcupado(LocalTime horaInicio, LocalTime horaFin, String estado) {
}

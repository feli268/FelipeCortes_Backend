package cl.jdvzaror.reservas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservaDto(Long id, Long usuarioId, Long dependenciaId, String dependencia, LocalDate fecha,
        LocalTime horaInicio, LocalTime horaFin, String motivo, String estado, String observacion,
        LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
}

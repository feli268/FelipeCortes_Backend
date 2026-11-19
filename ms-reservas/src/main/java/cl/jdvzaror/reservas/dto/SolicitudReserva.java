package cl.jdvzaror.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** claveCliente: identificador unico generado por el celular para no duplicar la solicitud al reintentar. */
public record SolicitudReserva(
        @NotNull Long dependenciaId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotBlank @Size(max = 200) String motivo,
        @Size(max = 64) String claveCliente) {
}

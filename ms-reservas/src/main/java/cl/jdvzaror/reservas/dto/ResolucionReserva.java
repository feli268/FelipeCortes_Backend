package cl.jdvzaror.reservas.dto;

import jakarta.validation.constraints.Size;

public record ResolucionReserva(@Size(max = 255) String observacion) {
}

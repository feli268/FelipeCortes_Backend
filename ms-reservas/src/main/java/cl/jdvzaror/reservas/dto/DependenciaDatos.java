package cl.jdvzaror.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DependenciaDatos(@NotBlank String nombre, String descripcion, String ubicacion,
        @Positive Integer capacidadReferencial, Boolean activa) {
}

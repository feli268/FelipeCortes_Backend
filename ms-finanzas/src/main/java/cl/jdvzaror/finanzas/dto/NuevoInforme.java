package cl.jdvzaror.finanzas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NuevoInforme(@NotNull LocalDate periodoInicio, @NotNull LocalDate periodoFin,
        @Size(max = 255) String observacion) {
}

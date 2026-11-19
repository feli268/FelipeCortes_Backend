package cl.jdvzaror.finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NuevoPago(
        @NotNull Long reservaId,
        @NotNull @Positive Long monto,
        @NotNull LocalDate fechaPago,
        @NotBlank @Size(max = 40) String medioInformado,
        @Size(max = 80) String referencia,
        @Size(max = 255) String observacion) {
}

package cl.jdvzaror.finanzas.dto;

import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Monto en pesos chilenos (entero, siempre positivo). claveCliente evita duplicar al reintentar. */
public record NuevoMovimiento(
        @NotNull TipoMovimiento tipo,
        @NotNull Long categoriaId,
        Long reservaId,
        @NotBlank @Size(max = 200) String descripcion,
        @NotNull @Positive Long monto,
        @NotNull LocalDate fechaMovimiento,
        @Size(max = 64) String claveCliente) {
}

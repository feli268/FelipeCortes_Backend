package cl.jdvzaror.finanzas.dto;

import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoriaDatos(@NotBlank String nombre, @NotNull TipoMovimiento tipo, String descripcion,
        Boolean activa) {
}

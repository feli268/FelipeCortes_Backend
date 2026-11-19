package cl.jdvzaror.admin.dto;

import jakarta.validation.constraints.NotNull;

public record CambioEstado(@NotNull Boolean activo) {
}

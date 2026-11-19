package cl.jdvzaror.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RecuperarSolicitud(@NotBlank String email) {
}

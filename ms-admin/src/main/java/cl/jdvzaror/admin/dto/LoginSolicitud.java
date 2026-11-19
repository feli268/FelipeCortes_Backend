package cl.jdvzaror.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(@NotBlank String email, @NotBlank String password) {
}

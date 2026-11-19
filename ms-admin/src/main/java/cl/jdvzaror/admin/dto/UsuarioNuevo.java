package cl.jdvzaror.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioNuevo(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        String telefono,
        @NotBlank @Size(min = 6, message = "debe tener al menos 6 caracteres") String password,
        @NotBlank String rol) {
}

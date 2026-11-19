package cl.jdvzaror.admin.dto;

public record UsuarioDto(Long id, String nombre, String email, String telefono, String rol, boolean activo) {
}

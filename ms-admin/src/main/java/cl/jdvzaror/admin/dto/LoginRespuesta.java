package cl.jdvzaror.admin.dto;

public record LoginRespuesta(String token, Long usuarioId, String nombre, String email, String rol) {
}

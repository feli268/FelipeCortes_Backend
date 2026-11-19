package cl.jdvzaror.reservas.dto;

public record DependenciaDto(Long id, String nombre, String descripcion, String ubicacion,
        Integer capacidadReferencial, boolean activa) {
}

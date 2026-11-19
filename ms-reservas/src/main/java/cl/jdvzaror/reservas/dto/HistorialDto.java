package cl.jdvzaror.reservas.dto;

import java.time.LocalDateTime;

public record HistorialDto(Long id, String estadoAnterior, String estadoNuevo, Long usuarioResponsable,
        String observacion, LocalDateTime fechaCambio) {
}

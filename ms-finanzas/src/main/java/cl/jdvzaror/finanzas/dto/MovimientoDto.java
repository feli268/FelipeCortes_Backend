package cl.jdvzaror.finanzas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimientoDto(Long id, String tipo, Long categoriaId, String categoria, Long reservaId,
        String descripcion, Long monto, LocalDate fechaMovimiento, Long usuarioRegistroId, String estado,
        LocalDateTime fechaCreacion) {
}

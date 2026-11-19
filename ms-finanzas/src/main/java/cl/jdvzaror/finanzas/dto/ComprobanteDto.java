package cl.jdvzaror.finanzas.dto;

import java.time.LocalDateTime;

public record ComprobanteDto(Long id, Long movimientoId, String nombreArchivo, String rutaReferencia,
        String tipoArchivo, LocalDateTime fechaAdjunto) {
}

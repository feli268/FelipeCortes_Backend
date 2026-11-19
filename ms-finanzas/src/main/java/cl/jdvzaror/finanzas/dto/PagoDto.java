package cl.jdvzaror.finanzas.dto;

import java.time.LocalDate;

public record PagoDto(Long id, Long reservaId, Long movimientoId, Long monto, LocalDate fechaPago,
        String medioInformado, String referencia, String estado, String observacion) {
}

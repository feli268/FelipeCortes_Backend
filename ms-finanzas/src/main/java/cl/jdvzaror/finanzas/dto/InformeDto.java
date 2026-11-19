package cl.jdvzaror.finanzas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InformeDto(Long id, LocalDate periodoInicio, LocalDate periodoFin, LocalDateTime fechaGeneracion,
        Long usuarioGeneradorId, String observacion, long ingresos, long gastos, long saldo) {
}

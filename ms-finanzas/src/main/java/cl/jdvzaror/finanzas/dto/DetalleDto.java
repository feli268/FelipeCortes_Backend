package cl.jdvzaror.finanzas.dto;

import java.util.List;

public record DetalleDto(ResumenDto resumen, List<TotalCategoria> ingresosPorCategoria,
        List<TotalCategoria> gastosPorCategoria) {
}

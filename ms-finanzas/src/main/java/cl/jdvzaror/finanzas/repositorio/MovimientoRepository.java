package cl.jdvzaror.finanzas.repositorio;

import cl.jdvzaror.finanzas.dto.TotalCategoria;
import cl.jdvzaror.finanzas.modelo.EstadoMovimiento;
import cl.jdvzaror.finanzas.modelo.MovimientoFinanciero;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoRepository extends JpaRepository<MovimientoFinanciero, Long> {

    List<MovimientoFinanciero> findByFechaMovimientoBetweenOrderByFechaMovimientoDescIdDesc(
            LocalDate desde, LocalDate hasta);

    Optional<MovimientoFinanciero> findByClaveCliente(String claveCliente);

    /** Suma solo movimientos del estado indicado (los anulados no se cuentan). 0 si no hay filas. */
    @Query("select coalesce(sum(m.monto), 0L) from MovimientoFinanciero m where m.tipo = :tipo "
            + "and m.estado = :estado and m.fechaMovimiento between :desde and :hasta")
    Long totalPorTipo(@Param("tipo") TipoMovimiento tipo, @Param("estado") EstadoMovimiento estado,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select new cl.jdvzaror.finanzas.dto.TotalCategoria(c.nombre, sum(m.monto)) from MovimientoFinanciero m "
            + "join m.categoria c where m.tipo = :tipo and m.estado = :estado "
            + "and m.fechaMovimiento between :desde and :hasta group by c.nombre order by sum(m.monto) desc")
    List<TotalCategoria> totalesPorCategoria(@Param("tipo") TipoMovimiento tipo,
            @Param("estado") EstadoMovimiento estado, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}

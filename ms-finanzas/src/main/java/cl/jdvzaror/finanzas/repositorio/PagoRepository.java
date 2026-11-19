package cl.jdvzaror.finanzas.repositorio;

import cl.jdvzaror.finanzas.modelo.EstadoPago;
import cl.jdvzaror.finanzas.modelo.Pago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByReservaIdAndEstado(Long reservaId, EstadoPago estado);

    List<Pago> findByReservaIdOrderByIdAsc(Long reservaId);

    List<Pago> findByMovimientoId(Long movimientoId);
}

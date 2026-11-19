package cl.jdvzaror.finanzas.repositorio;

import cl.jdvzaror.finanzas.modelo.Comprobante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    List<Comprobante> findByMovimientoIdOrderByIdAsc(Long movimientoId);
}

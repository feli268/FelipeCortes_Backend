package cl.jdvzaror.reservas.repositorio;

import cl.jdvzaror.reservas.modelo.HistorialReserva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialReservaRepository extends JpaRepository<HistorialReserva, Long> {

    List<HistorialReserva> findByReservaIdOrderByFechaCambioAscIdAsc(Long reservaId);
}

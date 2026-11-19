package cl.jdvzaror.reservas.repositorio;

import cl.jdvzaror.reservas.modelo.EstadoReserva;
import cl.jdvzaror.reservas.modelo.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByFechaDescHoraInicioDesc(Long usuarioId);

    List<Reserva> findByEstadoOrderByFechaAscHoraInicioAsc(EstadoReserva estado);

    List<Reserva> findAllByOrderByFechaDescHoraInicioDesc();

    Optional<Reserva> findByClaveCliente(String claveCliente);

    List<Reserva> findByDependenciaIdAndFechaAndEstadoInOrderByHoraInicioAsc(
            Long dependenciaId, LocalDate fecha, Collection<EstadoReserva> estados);

    /** Cuenta las reservas en ese estado que se superponen con [inicio, fin) el mismo dia y dependencia. */
    @Query("select count(r) from Reserva r where r.dependencia.id = :dependenciaId and r.fecha = :fecha "
            + "and r.estado = :estado and r.horaInicio < :fin and r.horaFin > :inicio and r.id <> :excluirId")
    long contarSuperpuestas(@Param("dependenciaId") Long dependenciaId, @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoReserva estado, @Param("inicio") LocalTime inicio,
            @Param("fin") LocalTime fin, @Param("excluirId") Long excluirId);
}

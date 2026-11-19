package cl.jdvzaror.reservas.servicio;

import cl.jdvzaror.reservas.dto.BloqueOcupado;
import cl.jdvzaror.reservas.dto.HistorialDto;
import cl.jdvzaror.reservas.dto.ReservaDto;
import cl.jdvzaror.reservas.dto.SolicitudReserva;
import cl.jdvzaror.reservas.modelo.Dependencia;
import cl.jdvzaror.reservas.modelo.EstadoReserva;
import cl.jdvzaror.reservas.modelo.HistorialReserva;
import cl.jdvzaror.reservas.modelo.Reserva;
import cl.jdvzaror.reservas.repositorio.DependenciaRepository;
import cl.jdvzaror.reservas.repositorio.HistorialReservaRepository;
import cl.jdvzaror.reservas.repositorio.ReservaRepository;
import cl.jdvzaror.reservas.seguridad.ApiException;
import cl.jdvzaror.reservas.seguridad.Sesion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio del ciclo de vida de una reserva. */
@Service
public class ReservaService {

    /** Id que nunca existe: se usa cuando no hay que excluir ninguna reserva al contar superposiciones. */
    private static final Long SIN_EXCLUIR = -1L;

    private final ReservaRepository reservas;
    private final DependenciaRepository dependencias;
    private final HistorialReservaRepository historial;
    private final Clock reloj;

    public ReservaService(ReservaRepository reservas, DependenciaRepository dependencias,
            HistorialReservaRepository historial, Clock reloj) {
        this.reservas = reservas;
        this.dependencias = dependencias;
        this.historial = historial;
        this.reloj = reloj;
    }

    @Transactional
    public ReservaDto solicitar(Sesion sesion, SolicitudReserva datos) {
        String clave = datos.claveCliente() == null || datos.claveCliente().isBlank() ? null : datos.claveCliente();
        if (clave != null) {
            Optional<Reserva> previa = reservas.findByClaveCliente(clave);
            if (previa.isPresent()) {
                if (!previa.get().getUsuarioId().equals(sesion.usuarioId())) {
                    throw ApiException.conflicto("La clave de solicitud ya fue usada");
                }
                return aDto(previa.get());
            }
        }
        validarHorario(datos.horaInicio(), datos.horaFin());
        if (datos.fecha().isBefore(LocalDate.now(reloj))) {
            throw ApiException.invalida("La fecha de la reserva no puede ser anterior a hoy");
        }
        Dependencia dependencia = dependencias.findById(datos.dependenciaId())
                .filter(Dependencia::isActiva)
                .orElseThrow(() -> ApiException.noEncontrado("La dependencia no existe o no esta disponible"));
        if (hayChoque(dependencia.getId(), datos.fecha(), datos.horaInicio(), datos.horaFin(), SIN_EXCLUIR)) {
            throw ApiException.conflicto("Ese horario ya esta reservado para la dependencia");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Reserva reserva = new Reserva();
        reserva.setUsuarioId(sesion.usuarioId());
        reserva.setDependencia(dependencia);
        reserva.setFecha(datos.fecha());
        reserva.setHoraInicio(datos.horaInicio());
        reserva.setHoraFin(datos.horaFin());
        reserva.setMotivo(datos.motivo().trim());
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setFechaCreacion(ahora);
        reserva.setFechaActualizacion(ahora);
        reserva.setClaveCliente(clave);
        reserva = reservas.save(reserva);
        registrarHistorial(reserva.getId(), null, EstadoReserva.PENDIENTE, sesion.usuarioId(), "Solicitud creada");
        return aDto(reserva);
    }

    @Transactional
    public ReservaDto aprobar(Sesion sesion, Long id, String observacion) {
        Reserva reserva = buscar(id);
        exigirPendiente(reserva);
        // La disponibilidad definitiva siempre se valida en el servidor antes de confirmar.
        if (hayChoque(reserva.getDependencia().getId(), reserva.getFecha(), reserva.getHoraInicio(),
                reserva.getHoraFin(), reserva.getId())) {
            throw ApiException.conflicto("No se puede aprobar: ya existe una reserva aprobada en ese horario");
        }
        return cambiarEstado(reserva, EstadoReserva.APROBADA, sesion, observacion);
    }

    @Transactional
    public ReservaDto rechazar(Sesion sesion, Long id, String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw ApiException.invalida("Debe indicar el motivo del rechazo");
        }
        Reserva reserva = buscar(id);
        exigirPendiente(reserva);
        return cambiarEstado(reserva, EstadoReserva.RECHAZADA, sesion, observacion);
    }

    @Transactional
    public ReservaDto cancelar(Sesion sesion, Long id, String observacion) {
        Reserva reserva = buscar(id);
        boolean esDuena = reserva.getUsuarioId().equals(sesion.usuarioId());
        if (!esDuena && !sesion.tieneRol("DIRECTIVA", "ADMIN")) {
            throw ApiException.prohibido("Solo quien solicito la reserva o la directiva puede cancelarla");
        }
        if (reserva.getEstado() != EstadoReserva.PENDIENTE && reserva.getEstado() != EstadoReserva.APROBADA) {
            throw ApiException.conflicto("La reserva ya esta cerrada y no se puede cancelar");
        }
        return cambiarEstado(reserva, EstadoReserva.CANCELADA, sesion, observacion);
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> misReservas(Sesion sesion) {
        return reservas.findByUsuarioIdOrderByFechaDescHoraInicioDesc(sesion.usuarioId()).stream()
                .map(ReservaService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> listar(EstadoReserva estado) {
        List<Reserva> lista = estado == null
                ? reservas.findAllByOrderByFechaDescHoraInicioDesc()
                : reservas.findByEstadoOrderByFechaAscHoraInicioAsc(estado);
        return lista.stream().map(ReservaService::aDto).toList();
    }

    /** Horarios ocupados (pendientes y aprobados) de una dependencia en una fecha, sin datos personales. */
    @Transactional(readOnly = true)
    public List<BloqueOcupado> disponibilidad(Long dependenciaId, LocalDate fecha) {
        return reservas.findByDependenciaIdAndFechaAndEstadoInOrderByHoraInicioAsc(dependenciaId, fecha,
                        List.of(EstadoReserva.PENDIENTE, EstadoReserva.APROBADA)).stream()
                .map(r -> new BloqueOcupado(r.getHoraInicio(), r.getHoraFin(), r.getEstado().name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialDto> historial(Sesion sesion, Long id) {
        Reserva reserva = buscar(id);
        if (!reserva.getUsuarioId().equals(sesion.usuarioId()) && !sesion.tieneRol("DIRECTIVA", "ADMIN")) {
            throw ApiException.prohibido("No puede ver el historial de esta reserva");
        }
        return historial.findByReservaIdOrderByFechaCambioAscIdAsc(id).stream()
                .map(h -> new HistorialDto(h.getId(),
                        h.getEstadoAnterior() == null ? null : h.getEstadoAnterior().name(),
                        h.getEstadoNuevo().name(), h.getUsuarioResponsable(), h.getObservacion(),
                        h.getFechaCambio()))
                .toList();
    }

    // ---- reglas internas ----

    private void validarHorario(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw ApiException.invalida("La hora de termino debe ser posterior a la hora de inicio");
        }
    }

    private boolean hayChoque(Long dependenciaId, LocalDate fecha, LocalTime inicio, LocalTime fin, Long excluirId) {
        return reservas.contarSuperpuestas(dependenciaId, fecha, EstadoReserva.APROBADA, inicio, fin, excluirId) > 0;
    }

    private void exigirPendiente(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw ApiException.conflicto("Solo se pueden resolver solicitudes pendientes");
        }
    }

    private Reserva buscar(Long id) {
        return reservas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Reserva no encontrada"));
    }

    private ReservaDto cambiarEstado(Reserva reserva, EstadoReserva nuevo, Sesion sesion, String observacion) {
        EstadoReserva anterior = reserva.getEstado();
        reserva.setEstado(nuevo);
        reserva.setObservacion(observacion);
        reserva.setFechaActualizacion(LocalDateTime.now(reloj));
        reserva = reservas.save(reserva);
        registrarHistorial(reserva.getId(), anterior, nuevo, sesion.usuarioId(), observacion);
        return aDto(reserva);
    }

    private void registrarHistorial(Long reservaId, EstadoReserva anterior, EstadoReserva nuevo,
            Long responsable, String observacion) {
        HistorialReserva h = new HistorialReserva();
        h.setReservaId(reservaId);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setUsuarioResponsable(responsable);
        h.setObservacion(observacion);
        h.setFechaCambio(LocalDateTime.now(reloj));
        historial.save(h);
    }

    static ReservaDto aDto(Reserva r) {
        return new ReservaDto(r.getId(), r.getUsuarioId(), r.getDependencia().getId(),
                r.getDependencia().getNombre(), r.getFecha(), r.getHoraInicio(), r.getHoraFin(), r.getMotivo(),
                r.getEstado().name(), r.getObservacion(), r.getFechaCreacion(), r.getFechaActualizacion());
    }
}

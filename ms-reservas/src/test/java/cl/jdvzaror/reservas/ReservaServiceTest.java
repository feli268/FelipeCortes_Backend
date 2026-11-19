package cl.jdvzaror.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import cl.jdvzaror.reservas.servicio.ReservaService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    @Mock
    ReservaRepository reservas;
    @Mock
    DependenciaRepository dependencias;
    @Mock
    HistorialReservaRepository historial;

    // "hoy" = 18-11-2026
    Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneId.of("America/Santiago"));
    ReservaService servicio;
    Sesion vecino = new Sesion(10L, "vecino@jdvzaror.cl", "VECINO");
    Sesion otroVecino = new Sesion(11L, "otro@jdvzaror.cl", "VECINO");
    Sesion directiva = new Sesion(20L, "directiva@jdvzaror.cl", "DIRECTIVA");
    Dependencia sede;
    LocalDate fecha = LocalDate.of(2026, 12, 5);

    @BeforeEach
    void preparar() {
        servicio = new ReservaService(reservas, dependencias, historial, reloj);
        sede = new Dependencia();
        sede.setId(1L);
        sede.setNombre("Sede social");
        sede.setActiva(true);
        when(dependencias.findById(1L)).thenReturn(Optional.of(sede));
        when(reservas.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(100L);
            }
            return r;
        });
        when(reservas.contarSuperpuestas(any(), any(), any(), any(), any(), any())).thenReturn(0L);
    }

    private SolicitudReserva solicitud(LocalDate dia, int horaIni, int horaFin) {
        return new SolicitudReserva(1L, dia, LocalTime.of(horaIni, 0), LocalTime.of(horaFin, 0), "Cumpleanos", null);
    }

    private Reserva pendiente() {
        Reserva r = new Reserva();
        r.setId(100L);
        r.setUsuarioId(10L);
        r.setDependencia(sede);
        r.setFecha(fecha);
        r.setHoraInicio(LocalTime.of(10, 0));
        r.setHoraFin(LocalTime.of(12, 0));
        r.setMotivo("Cumpleanos");
        r.setEstado(EstadoReserva.PENDIENTE);
        r.setFechaCreacion(LocalDateTime.now(reloj));
        r.setFechaActualizacion(LocalDateTime.now(reloj));
        when(reservas.findById(100L)).thenReturn(Optional.of(r));
        return r;
    }

    private void hayChoqueConAprobada() {
        when(reservas.contarSuperpuestas(any(), any(), eq(EstadoReserva.APROBADA), any(), any(), any()))
                .thenReturn(1L);
    }

    @Test
    void solicitudValidaQuedaPendienteYGuardaHistorial() {
        ReservaDto r = servicio.solicitar(vecino, solicitud(fecha, 10, 12));

        assertEquals("PENDIENTE", r.estado());
        assertEquals(10L, r.usuarioId());
        verify(historial).save(any(HistorialReserva.class));
    }

    @Test
    void horaDeTerminoDebeSerPosteriorALaDeInicio() {
        ApiException e = assertThrows(ApiException.class, () -> servicio.solicitar(vecino, solicitud(fecha, 12, 10)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
        verify(reservas, never()).save(any());
    }

    @Test
    void horaIgualDeInicioYTerminoTambienSeRechaza() {
        assertThrows(ApiException.class, () -> servicio.solicitar(vecino, solicitud(fecha, 10, 10)));
    }

    @Test
    void noSePuedeReservarEnUnaFechaPasada() {
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.solicitar(vecino, solicitud(LocalDate.of(2026, 11, 1), 10, 12)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void noSePuedeSolicitarUnHorarioYaAprobado() {
        hayChoqueConAprobada();

        ApiException e = assertThrows(ApiException.class, () -> servicio.solicitar(vecino, solicitud(fecha, 11, 13)));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void dependenciaInexistenteDa404() {
        SolicitudReserva datos = new SolicitudReserva(99L, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0), "x", null);

        ApiException e = assertThrows(ApiException.class, () -> servicio.solicitar(vecino, datos));

        assertEquals(HttpStatus.NOT_FOUND, e.getEstado());
    }

    @Test
    void reintentarConLaMismaClaveNoDuplicaLaReserva() {
        Reserva existente = pendiente();
        existente.setClaveCliente("abc-123");
        when(reservas.findByClaveCliente("abc-123")).thenReturn(Optional.of(existente));
        SolicitudReserva reintento = new SolicitudReserva(1L, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Cumpleanos", "abc-123");

        ReservaDto r = servicio.solicitar(vecino, reintento);

        assertEquals(100L, r.id());
        verify(reservas, never()).save(any());
    }

    @Test
    void aprobarCambiaElEstadoYDejaHistorial() {
        pendiente();

        ReservaDto r = servicio.aprobar(directiva, 100L, "Todo en orden");

        assertEquals("APROBADA", r.estado());
        verify(historial).save(any(HistorialReserva.class));
    }

    @Test
    void noSePuedeAprobarSiYaHayOtraAprobadaEnEseHorario() {
        pendiente();
        hayChoqueConAprobada();

        ApiException e = assertThrows(ApiException.class, () -> servicio.aprobar(directiva, 100L, null));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void rechazarExigeUnMotivo() {
        pendiente();

        ApiException e = assertThrows(ApiException.class, () -> servicio.rechazar(directiva, 100L, " "));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void rechazarConMotivoFunciona() {
        pendiente();

        ReservaDto r = servicio.rechazar(directiva, 100L, "Fecha ocupada por evento vecinal");

        assertEquals("RECHAZADA", r.estado());
        assertEquals("Fecha ocupada por evento vecinal", r.observacion());
    }

    @Test
    void unaReservaYaResueltaNoSeVuelveAResolver() {
        Reserva r = pendiente();
        r.setEstado(EstadoReserva.RECHAZADA);

        ApiException e = assertThrows(ApiException.class, () -> servicio.aprobar(directiva, 100L, null));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void otroVecinoNoPuedeCancelarMiReserva() {
        pendiente();

        ApiException e = assertThrows(ApiException.class, () -> servicio.cancelar(otroVecino, 100L, null));

        assertEquals(HttpStatus.FORBIDDEN, e.getEstado());
    }

    @Test
    void laDuenaPuedeCancelarSuReserva() {
        pendiente();

        assertEquals("CANCELADA", servicio.cancelar(vecino, 100L, "Ya no la necesito").estado());
    }

    @Test
    void laDirectivaPuedeCancelarCualquierReserva() {
        pendiente();

        assertEquals("CANCELADA", servicio.cancelar(directiva, 100L, null).estado());
    }

    @Test
    void soloLaDuenaOLaDirectivaVenElHistorial() {
        pendiente();

        assertThrows(ApiException.class, () -> servicio.historial(otroVecino, 100L));
        servicio.historial(vecino, 100L);
        servicio.historial(directiva, 100L);
    }
}

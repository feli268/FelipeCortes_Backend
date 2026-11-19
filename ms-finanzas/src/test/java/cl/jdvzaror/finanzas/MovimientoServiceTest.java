package cl.jdvzaror.finanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.jdvzaror.finanzas.dto.MovimientoDto;
import cl.jdvzaror.finanzas.dto.NuevoComprobante;
import cl.jdvzaror.finanzas.dto.NuevoMovimiento;
import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.EstadoMovimiento;
import cl.jdvzaror.finanzas.modelo.EstadoPago;
import cl.jdvzaror.finanzas.modelo.MovimientoFinanciero;
import cl.jdvzaror.finanzas.modelo.Pago;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.repositorio.CategoriaRepository;
import cl.jdvzaror.finanzas.repositorio.ComprobanteRepository;
import cl.jdvzaror.finanzas.repositorio.MovimientoRepository;
import cl.jdvzaror.finanzas.repositorio.PagoRepository;
import cl.jdvzaror.finanzas.seguridad.ApiException;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import cl.jdvzaror.finanzas.servicio.MovimientoService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
class MovimientoServiceTest {

    @Mock
    MovimientoRepository movimientos;
    @Mock
    CategoriaRepository categorias;
    @Mock
    PagoRepository pagos;
    @Mock
    ComprobanteRepository comprobantes;

    Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneId.of("America/Santiago"));
    MovimientoService servicio;
    Sesion tesoreria = new Sesion(30L, "tesoreria@jdvzaror.cl", "TESORERIA");
    CategoriaMovimiento arriendo;
    CategoriaMovimiento electricidad;
    LocalDate hoy = LocalDate.of(2026, 11, 18);

    @BeforeEach
    void preparar() {
        servicio = new MovimientoService(movimientos, categorias, pagos, comprobantes, reloj);
        arriendo = categoria(1L, "Arriendo", TipoMovimiento.INGRESO);
        electricidad = categoria(2L, "Consumo electrico", TipoMovimiento.GASTO);
        when(categorias.findById(1L)).thenReturn(Optional.of(arriendo));
        when(categorias.findById(2L)).thenReturn(Optional.of(electricidad));
        when(movimientos.save(any(MovimientoFinanciero.class))).thenAnswer(inv -> {
            MovimientoFinanciero m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(500L);
            }
            return m;
        });
    }

    private CategoriaMovimiento categoria(Long id, String nombre, TipoMovimiento tipo) {
        CategoriaMovimiento c = new CategoriaMovimiento();
        c.setId(id);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setActiva(true);
        return c;
    }

    private MovimientoFinanciero vigente(Long id) {
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setId(id);
        m.setTipo(TipoMovimiento.GASTO);
        m.setCategoria(electricidad);
        m.setDescripcion("Cuenta de luz");
        m.setMonto(25000L);
        m.setFechaMovimiento(hoy);
        m.setUsuarioRegistroId(30L);
        m.setEstado(EstadoMovimiento.VIGENTE);
        m.setFechaCreacion(LocalDateTime.now(reloj));
        when(movimientos.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    private NuevoMovimiento gasto(long monto) {
        return new NuevoMovimiento(TipoMovimiento.GASTO, 2L, null, "Cuenta de luz", monto, hoy, null);
    }

    @Test
    void registrarUnGastoValidoLoDejaVigente() {
        MovimientoDto m = servicio.registrar(tesoreria, gasto(25000));

        assertEquals("VIGENTE", m.estado());
        assertEquals(25000L, m.monto());
        assertEquals(30L, m.usuarioRegistroId());
    }

    @Test
    void elMontoDebeSerPositivo() {
        ApiException cero = assertThrows(ApiException.class, () -> servicio.registrar(tesoreria, gasto(0)));
        ApiException negativo = assertThrows(ApiException.class, () -> servicio.registrar(tesoreria, gasto(-500)));

        assertEquals(HttpStatus.BAD_REQUEST, cero.getEstado());
        assertEquals(HttpStatus.BAD_REQUEST, negativo.getEstado());
        verify(movimientos, never()).save(any());
    }

    @Test
    void laCategoriaDebeCorresponderAlTipo() {
        // una categoria de INGRESO (Arriendo) no sirve para registrar un GASTO
        NuevoMovimiento incoherente = new NuevoMovimiento(TipoMovimiento.GASTO, 1L, null, "x", 1000L, hoy, null);

        ApiException e = assertThrows(ApiException.class, () -> servicio.registrar(tesoreria, incoherente));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void categoriaInactivaSeRechaza() {
        electricidad.setActiva(false);

        assertThrows(ApiException.class, () -> servicio.registrar(tesoreria, gasto(1000)));
    }

    @Test
    void reintentarConLaMismaClaveNoDuplicaElMovimiento() {
        MovimientoFinanciero previo = vigente(77L);
        when(movimientos.findByClaveCliente("clave-1")).thenReturn(Optional.of(previo));
        NuevoMovimiento reintento = new NuevoMovimiento(TipoMovimiento.GASTO, 2L, null, "Cuenta de luz", 25000L,
                hoy, "clave-1");

        MovimientoDto m = servicio.registrar(tesoreria, reintento);

        assertEquals(77L, m.id());
        verify(movimientos, never()).save(any());
    }

    @Test
    void anularCambiaElEstadoYNoSeBorra() {
        MovimientoFinanciero m = vigente(10L);

        MovimientoDto r = servicio.anular(10L);

        assertEquals("ANULADO", r.estado());
        assertEquals(EstadoMovimiento.ANULADO, m.getEstado());
        verify(movimientos, never()).deleteById(any());
    }

    @Test
    void noSePuedeAnularDosVeces() {
        MovimientoFinanciero m = vigente(10L);
        m.setEstado(EstadoMovimiento.ANULADO);

        ApiException e = assertThrows(ApiException.class, () -> servicio.anular(10L));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void anularElIngresoDeUnPagoAnulaTambienElPago() {
        vigente(10L);
        Pago pago = new Pago();
        pago.setId(1L);
        pago.setMovimientoId(10L);
        pago.setEstado(EstadoPago.REGISTRADO);
        when(pagos.findByMovimientoId(10L)).thenReturn(List.of(pago));

        servicio.anular(10L);

        assertEquals(EstadoPago.ANULADO, pago.getEstado());
    }

    @Test
    void soloSeAceptanComprobantesJpgPngOPdf() {
        vigente(10L);
        NuevoComprobante ejecutable = new NuevoComprobante("virus.exe", "application/x-msdownload", "/ruta/virus.exe");

        ApiException e = assertThrows(ApiException.class, () -> servicio.adjuntarComprobante(10L, ejecutable));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void noSeAdjuntaComprobanteAUnMovimientoAnulado() {
        MovimientoFinanciero m = vigente(10L);
        m.setEstado(EstadoMovimiento.ANULADO);
        NuevoComprobante boleta = new NuevoComprobante("boleta.jpg", "image/jpeg", "/ruta/boleta.jpg");

        ApiException e = assertThrows(ApiException.class, () -> servicio.adjuntarComprobante(10L, boleta));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void periodoConFormatoIncorrectoDa400() {
        ApiException e = assertThrows(ApiException.class, () -> servicio.listar("noviembre"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }
}

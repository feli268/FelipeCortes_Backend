package cl.jdvzaror.finanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.jdvzaror.finanzas.dto.DetalleDto;
import cl.jdvzaror.finanzas.dto.MovimientoDto;
import cl.jdvzaror.finanzas.dto.NuevoMovimiento;
import cl.jdvzaror.finanzas.dto.NuevoPago;
import cl.jdvzaror.finanzas.dto.PagoDto;
import cl.jdvzaror.finanzas.dto.ResumenDto;
import cl.jdvzaror.finanzas.dto.TotalCategoria;
import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.EstadoMovimiento;
import cl.jdvzaror.finanzas.modelo.EstadoPago;
import cl.jdvzaror.finanzas.modelo.Pago;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.modelo.MovimientoFinanciero;
import cl.jdvzaror.finanzas.repositorio.CategoriaRepository;
import cl.jdvzaror.finanzas.repositorio.ComprobanteRepository;
import cl.jdvzaror.finanzas.repositorio.InformeRepository;
import cl.jdvzaror.finanzas.repositorio.MovimientoRepository;
import cl.jdvzaror.finanzas.repositorio.PagoRepository;
import cl.jdvzaror.finanzas.seguridad.ApiException;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import cl.jdvzaror.finanzas.servicio.InformeService;
import cl.jdvzaror.finanzas.servicio.MovimientoService;
import cl.jdvzaror.finanzas.servicio.PagoService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoYInformeServiceTest {

    @Mock
    PagoRepository pagos;
    @Mock
    CategoriaRepository categorias;
    @Mock
    MovimientoRepository movimientos;
    @Mock
    ComprobanteRepository comprobantes;
    @Mock
    InformeRepository informes;

    Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneId.of("America/Santiago"));
    Sesion tesoreria = new Sesion(30L, "tesoreria@jdvzaror.cl", "TESORERIA");
    LocalDate hoy = LocalDate.of(2026, 11, 18);

    // ---------- pagos informativos ----------

    private PagoService pagoService() {
        CategoriaMovimiento arriendo = new CategoriaMovimiento();
        arriendo.setId(1L);
        arriendo.setNombre("Arriendo");
        arriendo.setTipo(TipoMovimiento.INGRESO);
        arriendo.setActiva(true);
        when(categorias.findByNombreIgnoreCase("Arriendo")).thenReturn(Optional.of(arriendo));
        // Se usa el MovimientoService real (con repositorios simulados): asi se prueba la regla completa.
        when(categorias.findById(1L)).thenReturn(Optional.of(arriendo));
        when(movimientos.save(any(MovimientoFinanciero.class))).thenAnswer(inv -> {
            MovimientoFinanciero m = inv.getArgument(0);
            m.setId(900L);
            return m;
        });
        when(pagos.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        return new PagoService(pagos, categorias,
                new MovimientoService(movimientos, categorias, pagos, comprobantes, reloj));
    }

    @Test
    void registrarUnPagoCreaElIngresoAsociado() {
        PagoService servicio = pagoService();

        PagoDto p = servicio.registrar(tesoreria, new NuevoPago(5L, 60000L, hoy, "Transferencia", "ref-1", null));

        assertEquals(900L, p.movimientoId());
        assertEquals("REGISTRADO", p.estado());
        ArgumentCaptor<MovimientoFinanciero> captor = ArgumentCaptor.forClass(MovimientoFinanciero.class);
        verify(movimientos).save(captor.capture());
        assertEquals(TipoMovimiento.INGRESO, captor.getValue().getTipo());
        assertEquals(60000L, captor.getValue().getMonto());
        assertEquals(5L, captor.getValue().getReservaId());
        assertEquals(EstadoMovimiento.VIGENTE, captor.getValue().getEstado());
    }

    @Test
    void unaReservaNoPuedeTenerDosPagosVigentes() {
        PagoService servicio = pagoService();
        when(pagos.existsByReservaIdAndEstado(5L, EstadoPago.REGISTRADO)).thenReturn(true);

        ApiException e = assertThrows(ApiException.class,
                () -> servicio.registrar(tesoreria, new NuevoPago(5L, 60000L, hoy, "Efectivo", null, null)));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        verify(movimientos, never()).save(any());
    }

    @Test
    void elMontoDelPagoDebeSerPositivo() {
        PagoService servicio = pagoService();

        assertThrows(ApiException.class,
                () -> servicio.registrar(tesoreria, new NuevoPago(5L, 0L, hoy, "Efectivo", null, null)));
    }

    // ---------- informes ----------

    private InformeService informeService() {
        return new InformeService(movimientos, informes, reloj);
    }

    @Test
    void elSaldoEsIngresosMenosGastos() {
        when(movimientos.totalPorTipo(eq(TipoMovimiento.INGRESO), eq(EstadoMovimiento.VIGENTE), any(), any()))
                .thenReturn(150000L);
        when(movimientos.totalPorTipo(eq(TipoMovimiento.GASTO), eq(EstadoMovimiento.VIGENTE), any(), any()))
                .thenReturn(40000L);

        ResumenDto r = informeService().resumen("2026-11");

        assertEquals(150000L, r.ingresos());
        assertEquals(40000L, r.gastos());
        assertEquals(110000L, r.saldo());
        assertEquals("2026-11", r.periodo());
    }

    @Test
    void losTotalesSoloConsultanMovimientosVigentes() {
        when(movimientos.totalPorTipo(any(), any(), any(), any())).thenReturn(0L);

        informeService().resumen(null);

        // si algun dia se consulta con ANULADO, los totales contarian registros anulados
        verify(movimientos, never()).totalPorTipo(any(), eq(EstadoMovimiento.ANULADO), any(), any());
    }

    @Test
    void elDetalleIncluyeLosTotalesPorCategoria() {
        when(movimientos.totalPorTipo(any(), any(), any(), any())).thenReturn(10L);
        when(movimientos.totalesPorCategoria(eq(TipoMovimiento.GASTO), any(), any(), any()))
                .thenReturn(List.of(new TotalCategoria("Agua", 8000L)));
        when(movimientos.totalesPorCategoria(eq(TipoMovimiento.INGRESO), any(), any(), any()))
                .thenReturn(List.of());

        DetalleDto d = informeService().detalle("2026-11");

        assertEquals(1, d.gastosPorCategoria().size());
        assertEquals("Agua", d.gastosPorCategoria().get(0).categoria());
    }

    @Test
    void elPeriodoDelInformeNoPuedeTerminarAntesDeEmpezar() {
        ApiException e = assertThrows(ApiException.class, () -> informeService().generar(tesoreria,
                new cl.jdvzaror.finanzas.dto.NuevoInforme(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }
}

package cl.jdvzaror.finanzas.servicio;

import cl.jdvzaror.finanzas.dto.DetalleDto;
import cl.jdvzaror.finanzas.dto.InformeDto;
import cl.jdvzaror.finanzas.dto.NuevoInforme;
import cl.jdvzaror.finanzas.dto.ResumenDto;
import cl.jdvzaror.finanzas.modelo.EstadoMovimiento;
import cl.jdvzaror.finanzas.modelo.InformeFinanciero;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.repositorio.InformeRepository;
import cl.jdvzaror.finanzas.repositorio.MovimientoRepository;
import cl.jdvzaror.finanzas.seguridad.ApiException;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Totales calculados desde los movimientos vigentes; los anulados nunca se cuentan. */
@Service
public class InformeService {

    private final MovimientoRepository movimientos;
    private final InformeRepository informes;
    private final Clock reloj;

    public InformeService(MovimientoRepository movimientos, InformeRepository informes, Clock reloj) {
        this.movimientos = movimientos;
        this.informes = informes;
        this.reloj = reloj;
    }

    /** Resumen para vecinos: solo ingresos, gastos y saldo. */
    @Transactional(readOnly = true)
    public ResumenDto resumen(String periodo) {
        Periodo p = Periodo.de(periodo);
        return calcular(periodo == null || periodo.isBlank() ? "todos" : periodo.trim(), p.desde(), p.hasta());
    }

    /** Detalle para tesoreria y directiva: agrega los totales por categoria. */
    @Transactional(readOnly = true)
    public DetalleDto detalle(String periodo) {
        Periodo p = Periodo.de(periodo);
        ResumenDto resumen = calcular(periodo == null || periodo.isBlank() ? "todos" : periodo.trim(),
                p.desde(), p.hasta());
        return new DetalleDto(resumen,
                movimientos.totalesPorCategoria(TipoMovimiento.INGRESO, EstadoMovimiento.VIGENTE, p.desde(), p.hasta()),
                movimientos.totalesPorCategoria(TipoMovimiento.GASTO, EstadoMovimiento.VIGENTE, p.desde(), p.hasta()));
    }

    /** Guarda la cabecera de una rendicion y devuelve sus totales (los montos no se duplican). */
    @Transactional
    public InformeDto generar(Sesion sesion, NuevoInforme datos) {
        if (datos.periodoFin().isBefore(datos.periodoInicio())) {
            throw ApiException.invalida("El fin del periodo no puede ser anterior al inicio");
        }
        InformeFinanciero i = new InformeFinanciero();
        i.setPeriodoInicio(datos.periodoInicio());
        i.setPeriodoFin(datos.periodoFin());
        i.setFechaGeneracion(LocalDateTime.now(reloj));
        i.setUsuarioGeneradorId(sesion.usuarioId());
        i.setObservacion(datos.observacion());
        i = informes.save(i);
        ResumenDto r = calcular(datos.periodoInicio() + " a " + datos.periodoFin(),
                datos.periodoInicio(), datos.periodoFin());
        return new InformeDto(i.getId(), i.getPeriodoInicio(), i.getPeriodoFin(), i.getFechaGeneracion(),
                i.getUsuarioGeneradorId(), i.getObservacion(), r.ingresos(), r.gastos(), r.saldo());
    }

    private ResumenDto calcular(String etiqueta, LocalDate desde, LocalDate hasta) {
        long ingresos = movimientos.totalPorTipo(TipoMovimiento.INGRESO, EstadoMovimiento.VIGENTE, desde, hasta);
        long gastos = movimientos.totalPorTipo(TipoMovimiento.GASTO, EstadoMovimiento.VIGENTE, desde, hasta);
        return new ResumenDto(etiqueta, ingresos, gastos, ingresos - gastos);
    }
}

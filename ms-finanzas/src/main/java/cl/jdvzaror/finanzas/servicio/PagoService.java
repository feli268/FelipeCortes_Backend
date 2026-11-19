package cl.jdvzaror.finanzas.servicio;

import cl.jdvzaror.finanzas.dto.MovimientoDto;
import cl.jdvzaror.finanzas.dto.NuevoMovimiento;
import cl.jdvzaror.finanzas.dto.NuevoPago;
import cl.jdvzaror.finanzas.dto.PagoDto;
import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.EstadoPago;
import cl.jdvzaror.finanzas.modelo.Pago;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.repositorio.CategoriaRepository;
import cl.jdvzaror.finanzas.repositorio.PagoRepository;
import cl.jdvzaror.finanzas.seguridad.ApiException;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pagos informativos de arriendos. No procesan transacciones reales ni se integran con bancos. */
@Service
public class PagoService {

    static final String CATEGORIA_ARRIENDO = "Arriendo";

    private final PagoRepository pagos;
    private final CategoriaRepository categorias;
    private final MovimientoService movimientos;

    public PagoService(PagoRepository pagos, CategoriaRepository categorias, MovimientoService movimientos) {
        this.pagos = pagos;
        this.categorias = categorias;
        this.movimientos = movimientos;
    }

    /**
     * Registra el pago y crea el ingreso asociado (categoria Arriendo). Una reserva solo puede tener
     * un pago vigente, para no contabilizar dos veces el mismo ingreso.
     */
    @Transactional
    public PagoDto registrar(Sesion sesion, NuevoPago datos) {
        if (datos.monto() == null || datos.monto() <= 0) {
            throw ApiException.invalida("El monto debe ser mayor que cero");
        }
        if (pagos.existsByReservaIdAndEstado(datos.reservaId(), EstadoPago.REGISTRADO)) {
            throw ApiException.conflicto("La reserva ya tiene un pago registrado");
        }
        CategoriaMovimiento arriendo = categorias.findByNombreIgnoreCase(CATEGORIA_ARRIENDO)
                .filter(c -> c.getTipo() == TipoMovimiento.INGRESO)
                .orElseThrow(() -> ApiException.invalida("Falta la categoria de ingreso 'Arriendo'"));

        MovimientoDto ingreso = movimientos.registrar(sesion, new NuevoMovimiento(TipoMovimiento.INGRESO,
                arriendo.getId(), datos.reservaId(), "Pago informativo de arriendo, reserva " + datos.reservaId(),
                datos.monto(), datos.fechaPago(), null));

        Pago pago = new Pago();
        pago.setReservaId(datos.reservaId());
        pago.setMovimientoId(ingreso.id());
        pago.setMonto(datos.monto());
        pago.setFechaPago(datos.fechaPago());
        pago.setMedioInformado(datos.medioInformado().trim());
        pago.setReferencia(datos.referencia());
        pago.setEstado(EstadoPago.REGISTRADO);
        pago.setObservacion(datos.observacion());
        return aDto(pagos.save(pago));
    }

    @Transactional(readOnly = true)
    public List<PagoDto> porReserva(Long reservaId) {
        return pagos.findByReservaIdOrderByIdAsc(reservaId).stream().map(PagoService::aDto).toList();
    }

    static PagoDto aDto(Pago p) {
        return new PagoDto(p.getId(), p.getReservaId(), p.getMovimientoId(), p.getMonto(), p.getFechaPago(),
                p.getMedioInformado(), p.getReferencia(), p.getEstado().name(), p.getObservacion());
    }
}

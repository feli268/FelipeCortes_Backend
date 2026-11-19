package cl.jdvzaror.finanzas.servicio;

import cl.jdvzaror.finanzas.dto.ComprobanteDto;
import cl.jdvzaror.finanzas.dto.MovimientoDto;
import cl.jdvzaror.finanzas.dto.NuevoComprobante;
import cl.jdvzaror.finanzas.dto.NuevoMovimiento;
import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.Comprobante;
import cl.jdvzaror.finanzas.modelo.EstadoMovimiento;
import cl.jdvzaror.finanzas.modelo.EstadoPago;
import cl.jdvzaror.finanzas.modelo.MovimientoFinanciero;
import cl.jdvzaror.finanzas.repositorio.CategoriaRepository;
import cl.jdvzaror.finanzas.repositorio.ComprobanteRepository;
import cl.jdvzaror.finanzas.repositorio.MovimientoRepository;
import cl.jdvzaror.finanzas.repositorio.PagoRepository;
import cl.jdvzaror.finanzas.seguridad.ApiException;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de integridad de ingresos y gastos. */
@Service
public class MovimientoService {

    static final Set<String> TIPOS_ARCHIVO = Set.of("image/jpeg", "image/png", "application/pdf");

    private final MovimientoRepository movimientos;
    private final CategoriaRepository categorias;
    private final PagoRepository pagos;
    private final ComprobanteRepository comprobantes;
    private final Clock reloj;

    public MovimientoService(MovimientoRepository movimientos, CategoriaRepository categorias,
            PagoRepository pagos, ComprobanteRepository comprobantes, Clock reloj) {
        this.movimientos = movimientos;
        this.categorias = categorias;
        this.pagos = pagos;
        this.comprobantes = comprobantes;
        this.reloj = reloj;
    }

    @Transactional
    public MovimientoDto registrar(Sesion sesion, NuevoMovimiento datos) {
        String clave = datos.claveCliente() == null || datos.claveCliente().isBlank() ? null : datos.claveCliente();
        if (clave != null) {
            Optional<MovimientoFinanciero> previo = movimientos.findByClaveCliente(clave);
            if (previo.isPresent()) {
                return aDto(previo.get());
            }
        }
        if (datos.monto() == null || datos.monto() <= 0) {
            throw ApiException.invalida("El monto debe ser mayor que cero");
        }
        CategoriaMovimiento categoria = categorias.findById(datos.categoriaId())
                .filter(CategoriaMovimiento::isActiva)
                .orElseThrow(() -> ApiException.invalida("La categoria no existe o esta inactiva"));
        if (categoria.getTipo() != datos.tipo()) {
            throw ApiException.invalida("La categoria no corresponde al tipo de movimiento");
        }
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setTipo(datos.tipo());
        m.setCategoria(categoria);
        m.setReservaId(datos.reservaId());
        m.setDescripcion(datos.descripcion().trim());
        m.setMonto(datos.monto());
        m.setFechaMovimiento(datos.fechaMovimiento());
        m.setUsuarioRegistroId(sesion.usuarioId());
        m.setEstado(EstadoMovimiento.VIGENTE);
        m.setFechaCreacion(LocalDateTime.now(reloj));
        m.setClaveCliente(clave);
        return aDto(movimientos.save(m));
    }

    /** Un movimiento no se borra: se anula, y deja de contarse en los totales. */
    @Transactional
    public MovimientoDto anular(Long id) {
        MovimientoFinanciero m = buscar(id);
        if (m.getEstado() == EstadoMovimiento.ANULADO) {
            throw ApiException.conflicto("El movimiento ya esta anulado");
        }
        m.setEstado(EstadoMovimiento.ANULADO);
        movimientos.save(m);
        // Si era el ingreso de un pago informativo, el pago tambien queda anulado (evita contarlo dos veces).
        pagos.findByMovimientoId(id).forEach(p -> {
            p.setEstado(EstadoPago.ANULADO);
            pagos.save(p);
        });
        return aDto(m);
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> listar(String periodo) {
        Periodo p = Periodo.de(periodo);
        return movimientos.findByFechaMovimientoBetweenOrderByFechaMovimientoDescIdDesc(p.desde(), p.hasta())
                .stream().map(MovimientoService::aDto).toList();
    }

    @Transactional
    public ComprobanteDto adjuntarComprobante(Long movimientoId, NuevoComprobante datos) {
        MovimientoFinanciero m = buscar(movimientoId);
        if (m.getEstado() == EstadoMovimiento.ANULADO) {
            throw ApiException.conflicto("No se pueden adjuntar comprobantes a un movimiento anulado");
        }
        if (!TIPOS_ARCHIVO.contains(datos.tipoArchivo().toLowerCase())) {
            throw ApiException.invalida("Tipo de archivo no permitido (use JPG, PNG o PDF)");
        }
        Comprobante c = new Comprobante();
        c.setMovimientoId(movimientoId);
        c.setNombreArchivo(datos.nombreArchivo().trim());
        c.setTipoArchivo(datos.tipoArchivo().toLowerCase());
        c.setRutaReferencia(datos.rutaReferencia().trim());
        c.setFechaAdjunto(LocalDateTime.now(reloj));
        return aDto(comprobantes.save(c));
    }

    @Transactional(readOnly = true)
    public List<ComprobanteDto> comprobantes(Long movimientoId) {
        buscar(movimientoId);
        return comprobantes.findByMovimientoIdOrderByIdAsc(movimientoId).stream()
                .map(MovimientoService::aDto).toList();
    }

    private MovimientoFinanciero buscar(Long id) {
        return movimientos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Movimiento no encontrado"));
    }

    static MovimientoDto aDto(MovimientoFinanciero m) {
        return new MovimientoDto(m.getId(), m.getTipo().name(), m.getCategoria().getId(),
                m.getCategoria().getNombre(), m.getReservaId(), m.getDescripcion(), m.getMonto(),
                m.getFechaMovimiento(), m.getUsuarioRegistroId(), m.getEstado().name(), m.getFechaCreacion());
    }

    static ComprobanteDto aDto(Comprobante c) {
        return new ComprobanteDto(c.getId(), c.getMovimientoId(), c.getNombreArchivo(), c.getRutaReferencia(),
                c.getTipoArchivo(), c.getFechaAdjunto());
    }
}

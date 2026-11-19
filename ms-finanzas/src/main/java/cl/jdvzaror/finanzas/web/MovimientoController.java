package cl.jdvzaror.finanzas.web;

import cl.jdvzaror.finanzas.dto.ComprobanteDto;
import cl.jdvzaror.finanzas.dto.MovimientoDto;
import cl.jdvzaror.finanzas.dto.NuevoComprobante;
import cl.jdvzaror.finanzas.dto.NuevoMovimiento;
import cl.jdvzaror.finanzas.servicio.MovimientoService;
import cl.jdvzaror.finanzas.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.finanzas.seguridad.RequiereRol;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private static final String SESION = AutenticacionInterceptor.ATRIBUTO_SESION;

    private final MovimientoService servicio;

    public MovimientoController(MovimientoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({"TESORERIA", "ADMIN"})
    public MovimientoDto registrar(@RequestAttribute(SESION) Sesion sesion,
            @Valid @RequestBody NuevoMovimiento datos) {
        return servicio.registrar(sesion, datos);
    }

    /** El detalle de movimientos nunca se expone a los vecinos. */
    @GetMapping
    @RequiereRol({"TESORERIA", "DIRECTIVA", "ADMIN"})
    public List<MovimientoDto> listar(@RequestParam(required = false) String periodo) {
        return servicio.listar(periodo);
    }

    @PutMapping("/{id}/anular")
    @RequiereRol({"TESORERIA", "ADMIN"})
    public MovimientoDto anular(@PathVariable Long id) {
        return servicio.anular(id);
    }

    @PostMapping("/{id}/comprobantes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({"TESORERIA", "ADMIN"})
    public ComprobanteDto adjuntar(@PathVariable Long id, @Valid @RequestBody NuevoComprobante datos) {
        return servicio.adjuntarComprobante(id, datos);
    }

    @GetMapping("/{id}/comprobantes")
    @RequiereRol({"TESORERIA", "DIRECTIVA", "ADMIN"})
    public List<ComprobanteDto> comprobantes(@PathVariable Long id) {
        return servicio.comprobantes(id);
    }
}

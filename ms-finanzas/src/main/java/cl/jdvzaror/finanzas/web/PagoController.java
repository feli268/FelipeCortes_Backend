package cl.jdvzaror.finanzas.web;

import cl.jdvzaror.finanzas.dto.NuevoPago;
import cl.jdvzaror.finanzas.dto.PagoDto;
import cl.jdvzaror.finanzas.servicio.PagoService;
import cl.jdvzaror.finanzas.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.finanzas.seguridad.RequiereRol;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private static final String SESION = AutenticacionInterceptor.ATRIBUTO_SESION;

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({"TESORERIA", "ADMIN"})
    public PagoDto registrar(@RequestAttribute(SESION) Sesion sesion, @Valid @RequestBody NuevoPago datos) {
        return servicio.registrar(sesion, datos);
    }

    @GetMapping
    @RequiereRol({"TESORERIA", "DIRECTIVA", "ADMIN"})
    public List<PagoDto> porReserva(@RequestParam Long reservaId) {
        return servicio.porReserva(reservaId);
    }
}

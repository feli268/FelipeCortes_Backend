package cl.jdvzaror.finanzas.web;

import cl.jdvzaror.finanzas.dto.DetalleDto;
import cl.jdvzaror.finanzas.dto.InformeDto;
import cl.jdvzaror.finanzas.dto.NuevoInforme;
import cl.jdvzaror.finanzas.dto.ResumenDto;
import cl.jdvzaror.finanzas.servicio.InformeService;
import cl.jdvzaror.finanzas.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.finanzas.seguridad.RequiereRol;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private static final String SESION = AutenticacionInterceptor.ATRIBUTO_SESION;

    private final InformeService servicio;

    public InformeController(InformeService servicio) {
        this.servicio = servicio;
    }

    /** Transparencia: cualquier usuario autenticado, incluidos los vecinos, ve solo los totales. */
    @GetMapping("/resumen")
    public ResumenDto resumen(@RequestParam(required = false) String periodo) {
        return servicio.resumen(periodo);
    }

    @GetMapping("/detalle")
    @RequiereRol({"TESORERIA", "DIRECTIVA", "ADMIN"})
    public DetalleDto detalle(@RequestParam(required = false) String periodo) {
        return servicio.detalle(periodo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({"TESORERIA", "ADMIN"})
    public InformeDto generar(@RequestAttribute(SESION) Sesion sesion, @Valid @RequestBody NuevoInforme datos) {
        return servicio.generar(sesion, datos);
    }
}

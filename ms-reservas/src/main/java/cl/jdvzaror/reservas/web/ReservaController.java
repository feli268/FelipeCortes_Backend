package cl.jdvzaror.reservas.web;

import cl.jdvzaror.reservas.dto.BloqueOcupado;
import cl.jdvzaror.reservas.dto.HistorialDto;
import cl.jdvzaror.reservas.dto.ReservaDto;
import cl.jdvzaror.reservas.dto.ResolucionReserva;
import cl.jdvzaror.reservas.dto.SolicitudReserva;
import cl.jdvzaror.reservas.modelo.EstadoReserva;
import cl.jdvzaror.reservas.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.reservas.seguridad.RequiereRol;
import cl.jdvzaror.reservas.seguridad.Sesion;
import cl.jdvzaror.reservas.servicio.ReservaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private static final String SESION = AutenticacionInterceptor.ATRIBUTO_SESION;

    private final ReservaService servicio;

    public ReservaController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({"VECINO", "ADMIN"})
    public ReservaDto solicitar(@RequestAttribute(SESION) Sesion sesion, @Valid @RequestBody SolicitudReserva datos) {
        return servicio.solicitar(sesion, datos);
    }

    @GetMapping("/mias")
    public List<ReservaDto> mias(@RequestAttribute(SESION) Sesion sesion) {
        return servicio.misReservas(sesion);
    }

    @GetMapping
    @RequiereRol({"DIRECTIVA", "TESORERIA", "ADMIN"})
    public List<ReservaDto> listar(@RequestParam(required = false) EstadoReserva estado) {
        return servicio.listar(estado);
    }

    @GetMapping("/disponibilidad")
    public List<BloqueOcupado> disponibilidad(@RequestParam Long dependenciaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.disponibilidad(dependenciaId, fecha);
    }

    @PutMapping("/{id}/aprobar")
    @RequiereRol({"DIRECTIVA", "ADMIN"})
    public ReservaDto aprobar(@RequestAttribute(SESION) Sesion sesion, @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolucionReserva cuerpo) {
        return servicio.aprobar(sesion, id, observacionDe(cuerpo));
    }

    @PutMapping("/{id}/rechazar")
    @RequiereRol({"DIRECTIVA", "ADMIN"})
    public ReservaDto rechazar(@RequestAttribute(SESION) Sesion sesion, @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolucionReserva cuerpo) {
        return servicio.rechazar(sesion, id, observacionDe(cuerpo));
    }

    @PutMapping("/{id}/cancelar")
    public ReservaDto cancelar(@RequestAttribute(SESION) Sesion sesion, @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolucionReserva cuerpo) {
        return servicio.cancelar(sesion, id, observacionDe(cuerpo));
    }

    @GetMapping("/{id}/historial")
    public List<HistorialDto> historial(@RequestAttribute(SESION) Sesion sesion, @PathVariable Long id) {
        return servicio.historial(sesion, id);
    }

    private static String observacionDe(ResolucionReserva cuerpo) {
        return cuerpo == null ? null : cuerpo.observacion();
    }
}

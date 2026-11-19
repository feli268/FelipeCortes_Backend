package cl.jdvzaror.reservas.web;

import cl.jdvzaror.reservas.dto.DependenciaDatos;
import cl.jdvzaror.reservas.dto.DependenciaDto;
import cl.jdvzaror.reservas.seguridad.RequiereRol;
import cl.jdvzaror.reservas.servicio.DependenciaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dependencias")
public class DependenciaController {

    private final DependenciaService servicio;

    public DependenciaController(DependenciaService servicio) {
        this.servicio = servicio;
    }

    /** Dependencias disponibles para arriendo (cualquier usuario autenticado). */
    @GetMapping
    public List<DependenciaDto> listar() {
        return servicio.listarActivas();
    }

    @GetMapping("/todas")
    @RequiereRol("ADMIN")
    public List<DependenciaDto> todas() {
        return servicio.listarTodas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol("ADMIN")
    public DependenciaDto crear(@Valid @RequestBody DependenciaDatos datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    @RequiereRol("ADMIN")
    public DependenciaDto actualizar(@PathVariable Long id, @Valid @RequestBody DependenciaDatos datos) {
        return servicio.actualizar(id, datos);
    }
}

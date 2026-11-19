package cl.jdvzaror.finanzas.web;

import cl.jdvzaror.finanzas.dto.CategoriaDatos;
import cl.jdvzaror.finanzas.dto.CategoriaDto;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.servicio.CategoriaService;
import cl.jdvzaror.finanzas.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.finanzas.seguridad.RequiereRol;
import cl.jdvzaror.finanzas.seguridad.Sesion;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaDto> listar(@RequestParam(required = false) TipoMovimiento tipo) {
        return servicio.listarActivas(tipo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol("ADMIN")
    public CategoriaDto crear(@Valid @RequestBody CategoriaDatos datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    @RequiereRol("ADMIN")
    public CategoriaDto actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDatos datos) {
        return servicio.actualizar(id, datos);
    }
}

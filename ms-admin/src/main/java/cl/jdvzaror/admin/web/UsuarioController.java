package cl.jdvzaror.admin.web;

import cl.jdvzaror.admin.dto.CambioEstado;
import cl.jdvzaror.admin.dto.RolDto;
import cl.jdvzaror.admin.dto.UsuarioDto;
import cl.jdvzaror.admin.dto.UsuarioNuevo;
import cl.jdvzaror.admin.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.admin.seguridad.RequiereRol;
import cl.jdvzaror.admin.seguridad.Sesion;
import cl.jdvzaror.admin.servicio.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administracion de cuentas y perfiles: solo el administrador del sistema. */
@RestController
@RequestMapping("/api")
@RequiereRol("ADMIN")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios")
    public List<UsuarioDto> listar() {
        return servicio.listar();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody UsuarioNuevo datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/usuarios/{id}/estado")
    public UsuarioDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstado cambio,
            @RequestAttribute(AutenticacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        return servicio.cambiarEstado(id, cambio.activo(), sesion);
    }

    @GetMapping("/roles")
    public List<RolDto> roles() {
        return servicio.listarRoles();
    }
}

package cl.jdvzaror.admin.web;

import cl.jdvzaror.admin.dto.LoginRespuesta;
import cl.jdvzaror.admin.dto.LoginSolicitud;
import cl.jdvzaror.admin.dto.MensajeRespuesta;
import cl.jdvzaror.admin.dto.RecuperarSolicitud;
import cl.jdvzaror.admin.seguridad.AutenticacionInterceptor;
import cl.jdvzaror.admin.seguridad.Sesion;
import cl.jdvzaror.admin.servicio.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginRespuesta login(@Valid @RequestBody LoginSolicitud solicitud) {
        return auth.iniciarSesion(solicitud.email(), solicitud.password());
    }

    @PostMapping("/recuperar")
    public MensajeRespuesta recuperar(@Valid @RequestBody RecuperarSolicitud solicitud) {
        return new MensajeRespuesta(auth.recuperarContrasena(solicitud.email()));
    }

    /** Devuelve los datos de la sesion del token enviado (sirve para comprobar que el token es valido). */
    @GetMapping("/yo")
    public Sesion yo(@RequestAttribute(AutenticacionInterceptor.ATRIBUTO_SESION) Sesion sesion) {
        return sesion;
    }
}

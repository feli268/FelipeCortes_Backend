package cl.jdvzaror.admin.servicio;

import cl.jdvzaror.admin.dto.LoginRespuesta;
import cl.jdvzaror.admin.modelo.Usuario;
import cl.jdvzaror.admin.repositorio.UsuarioRepository;
import cl.jdvzaror.admin.seguridad.ApiException;
import cl.jdvzaror.admin.seguridad.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final String MENSAJE_RECUPERACION =
            "Si el correo esta registrado, recibira instrucciones para recuperar su contrasena.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    /** Mismo error si el correo no existe, la clave es incorrecta o la cuenta esta inactiva. */
    @Transactional(readOnly = true)
    public LoginRespuesta iniciarSesion(String email, String password) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (usuario == null || !usuario.isActivo() || !encoder.matches(password, usuario.getPasswordHash())) {
            throw ApiException.noAutenticado("Credenciales invalidas");
        }
        String rol = usuario.getRol().getCodigo();
        String token = tokens.emitir(usuario.getId(), usuario.getEmail(), rol);
        return new LoginRespuesta(token, usuario.getId(), usuario.getNombre(), usuario.getEmail(), rol);
    }

    /**
     * Recuperacion SIMULADA: no se envia ningun correo y la respuesta es la misma exista o no la cuenta,
     * para no revelar que correos estan registrados.
     */
    public String recuperarContrasena(String email) {
        return MENSAJE_RECUPERACION;
    }
}

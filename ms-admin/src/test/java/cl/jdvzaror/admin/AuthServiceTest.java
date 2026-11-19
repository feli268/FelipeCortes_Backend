package cl.jdvzaror.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cl.jdvzaror.admin.dto.LoginRespuesta;
import cl.jdvzaror.admin.modelo.Rol;
import cl.jdvzaror.admin.modelo.Usuario;
import cl.jdvzaror.admin.repositorio.UsuarioRepository;
import cl.jdvzaror.admin.seguridad.ApiException;
import cl.jdvzaror.admin.seguridad.TokenService;
import cl.jdvzaror.admin.servicio.AuthService;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarios;

    PasswordEncoder encoder = new BCryptPasswordEncoder();
    TokenService tokens = new TokenService("secreto-de-prueba-0123456789-abcdefghij", 60, Clock.systemUTC());
    AuthService auth;
    Usuario vecino;

    @BeforeEach
    void preparar() {
        auth = new AuthService(usuarios, encoder, tokens);
        Rol rol = new Rol();
        rol.setCodigo("VECINO");
        vecino = new Usuario();
        vecino.setId(1L);
        vecino.setNombre("Vecino de prueba");
        vecino.setEmail("vecino@jdvzaror.cl");
        vecino.setPasswordHash(encoder.encode("1234"));
        vecino.setRol(rol);
        vecino.setActivo(true);
        when(usuarios.findByEmailIgnoreCase("vecino@jdvzaror.cl")).thenReturn(Optional.of(vecino));
        when(usuarios.findByEmailIgnoreCase("nadie@jdvzaror.cl")).thenReturn(Optional.empty());
    }

    @Test
    void loginCorrectoEntregaTokenConElRol() {
        LoginRespuesta r = auth.iniciarSesion("vecino@jdvzaror.cl", "1234");

        assertEquals("VECINO", r.rol());
        assertTrue(tokens.validar(r.token()).isPresent());
    }

    @Test
    void laContrasenaSeGuardaConHashNoEnTextoPlano() {
        assertNotEquals("1234", vecino.getPasswordHash());
        assertTrue(vecino.getPasswordHash().startsWith("$2"));
    }

    @Test
    void contrasenaIncorrectaDa401() {
        ApiException e = assertThrows(ApiException.class, () -> auth.iniciarSesion("vecino@jdvzaror.cl", "mala"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getEstado());
    }

    @Test
    void correoInexistenteDaElMismoErrorQueContrasenaIncorrecta() {
        ApiException sinCuenta = assertThrows(ApiException.class, () -> auth.iniciarSesion("nadie@jdvzaror.cl", "1234"));
        ApiException claveMala = assertThrows(ApiException.class, () -> auth.iniciarSesion("vecino@jdvzaror.cl", "mala"));

        assertEquals(claveMala.getMessage(), sinCuenta.getMessage());
        assertEquals(claveMala.getEstado(), sinCuenta.getEstado());
    }

    @Test
    void cuentaInactivaNoPuedeEntrar() {
        vecino.setActivo(false);

        assertThrows(ApiException.class, () -> auth.iniciarSesion("vecino@jdvzaror.cl", "1234"));
    }

    @Test
    void recuperarContrasenaRespondeIgualExistaONoLaCuenta() {
        assertEquals(auth.recuperarContrasena("vecino@jdvzaror.cl"), auth.recuperarContrasena("nadie@jdvzaror.cl"));
        assertEquals(AuthService.MENSAJE_RECUPERACION, auth.recuperarContrasena("cualquiera@jdvzaror.cl"));
    }
}

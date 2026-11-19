package cl.jdvzaror.admin.configuracion;

import cl.jdvzaror.admin.modelo.Rol;
import cl.jdvzaror.admin.modelo.Usuario;
import cl.jdvzaror.admin.repositorio.RolRepository;
import cl.jdvzaror.admin.repositorio.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Carga los 4 roles y 4 cuentas de prueba (datos ficticios) cuando la base esta vacia. */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Clock reloj;

    public DatosIniciales(RolRepository roles, UsuarioRepository usuarios, PasswordEncoder encoder, Clock reloj) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.reloj = reloj;
    }

    @Override
    public void run(String... args) {
        if (roles.count() == 0) {
            crearRol("VECINO", "Vecino/a", "Solicita arriendos y consulta el resumen financiero");
            crearRol("DIRECTIVA", "Directiva / Secretaria", "Revisa y resuelve las solicitudes de reserva");
            crearRol("TESORERIA", "Tesoreria", "Registra pagos, ingresos, gastos y comprobantes");
            crearRol("ADMIN", "Administrador del sistema", "Administra cuentas, perfiles y catalogos");
        }
        if (usuarios.count() == 0) {
            crearUsuario("Vecino de prueba", "vecino@jdvzaror.cl", "1234", "VECINO");
            crearUsuario("Directiva de prueba", "directiva@jdvzaror.cl", "directiva123", "DIRECTIVA");
            crearUsuario("Tesoreria de prueba", "tesoreria@jdvzaror.cl", "tesoreria123", "TESORERIA");
            crearUsuario("Admin de prueba", "admin@jdvzaror.cl", "admin123", "ADMIN");
        }
    }

    private void crearRol(String codigo, String nombre, String descripcion) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        roles.save(rol);
    }

    private void crearUsuario(String nombre, String email, String clave, String codigoRol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(clave));
        u.setRol(roles.findByCodigo(codigoRol).orElseThrow());
        u.setActivo(true);
        u.setFechaCreacion(LocalDateTime.now(reloj));
        usuarios.save(u);
    }
}

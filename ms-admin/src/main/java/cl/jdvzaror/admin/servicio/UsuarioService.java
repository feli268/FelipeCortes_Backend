package cl.jdvzaror.admin.servicio;

import cl.jdvzaror.admin.dto.RolDto;
import cl.jdvzaror.admin.dto.UsuarioDto;
import cl.jdvzaror.admin.dto.UsuarioNuevo;
import cl.jdvzaror.admin.modelo.Rol;
import cl.jdvzaror.admin.modelo.Usuario;
import cl.jdvzaror.admin.repositorio.RolRepository;
import cl.jdvzaror.admin.repositorio.UsuarioRepository;
import cl.jdvzaror.admin.seguridad.ApiException;
import cl.jdvzaror.admin.seguridad.Sesion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder, Clock reloj) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findAll().stream().map(UsuarioService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RolDto> listarRoles() {
        return roles.findAll().stream()
                .map(r -> new RolDto(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion()))
                .toList();
    }

    @Transactional
    public UsuarioDto crear(UsuarioNuevo datos) {
        String email = datos.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflicto("Ya existe una cuenta con ese correo");
        }
        Rol rol = roles.findByCodigo(datos.rol().trim().toUpperCase())
                .orElseThrow(() -> ApiException.invalida("El rol indicado no existe"));
        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setEmail(email);
        usuario.setTelefono(datos.telefono());
        usuario.setPasswordHash(encoder.encode(datos.password()));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now(reloj));
        return aDto(usuarios.save(usuario));
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean activo, Sesion sesion) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
        if (!activo && usuario.getId().equals(sesion.usuarioId())) {
            throw ApiException.invalida("No puede desactivar su propia cuenta");
        }
        usuario.setActivo(activo);
        return aDto(usuarios.save(usuario));
    }

    static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getEmail(), u.getTelefono(),
                u.getRol().getCodigo(), u.isActivo());
    }
}

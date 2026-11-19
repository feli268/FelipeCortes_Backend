package cl.jdvzaror.reservas.seguridad;

/** Datos del usuario autenticado, sacados del token. */
public record Sesion(Long usuarioId, String email, String rol) {

    public boolean tieneRol(String... roles) {
        for (String r : roles) {
            if (r.equals(rol)) {
                return true;
            }
        }
        return false;
    }
}

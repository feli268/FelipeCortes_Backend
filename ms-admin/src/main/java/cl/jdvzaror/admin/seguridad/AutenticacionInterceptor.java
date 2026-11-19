package cl.jdvzaror.admin.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Autentica y autoriza en el backend: valida el token "Authorization: Bearer ..." y,
 * si el endpoint tiene @RequiereRol, comprueba que el rol este permitido.
 */
public class AutenticacionInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "sesion";
    private final TokenService tokens;

    public AutenticacionInterceptor(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            throw ApiException.noAutenticado("Debe iniciar sesion");
        }
        Sesion sesion = tokens.validar(cabecera.substring(7))
                .orElseThrow(() -> ApiException.noAutenticado("Sesion invalida o expirada"));

        RequiereRol requisito = metodo.getMethodAnnotation(RequiereRol.class);
        if (requisito == null) {
            requisito = metodo.getBeanType().getAnnotation(RequiereRol.class);
        }
        if (requisito != null && !sesion.tieneRol(requisito.value())) {
            throw ApiException.prohibido("Su perfil no tiene permiso para esta operacion");
        }
        request.setAttribute(ATRIBUTO_SESION, sesion);
        return true;
    }
}

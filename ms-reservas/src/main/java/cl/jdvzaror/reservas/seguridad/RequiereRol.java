package cl.jdvzaror.reservas.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Roles que pueden llamar al endpoint. Sin esta anotacion basta con estar autenticado. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiereRol {
    String[] value();
}

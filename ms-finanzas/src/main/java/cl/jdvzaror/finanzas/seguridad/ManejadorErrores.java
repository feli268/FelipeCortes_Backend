package cl.jdvzaror.finanzas.seguridad;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Convierte las excepciones en respuestas JSON {"estado":..., "mensaje":...}. */
@RestControllerAdvice
public class ManejadorErrores {

    public record ErrorApi(int estado, String mensaje) {
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorApi> api(ApiException e) {
        return ResponseEntity.status(e.getEstado()).body(new ErrorApi(e.getEstado().value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorApi(400, detalle));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorApi> cuerpoInvalido(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorApi(400, "Datos con formato invalido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception e) {
        // Errores propios de Spring MVC (ruta inexistente, metodo no permitido, parametro faltante...)
        if (e instanceof ErrorResponse respuesta) {
            int codigo = respuesta.getStatusCode().value();
            return ResponseEntity.status(codigo).body(new ErrorApi(codigo, "Solicitud no valida"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApi(500, "Error interno del servidor"));
    }
}

package cl.jdvzaror.finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solo metadatos del respaldo: el archivo real queda en el dispositivo (se usan archivos ficticios). */
public record NuevoComprobante(
        @NotBlank @Size(max = 120) String nombreArchivo,
        @NotBlank @Size(max = 40) String tipoArchivo,
        @NotBlank @Size(max = 255) String rutaReferencia) {
}

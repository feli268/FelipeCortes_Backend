package cl.jdvzaror.finanzas.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long movimientoId;

    @Column(nullable = false, length = 120)
    private String nombreArchivo;

    @Column(nullable = false, length = 255)
    private String rutaReferencia;

    @Column(nullable = false, length = 40)
    private String tipoArchivo;

    @Column(nullable = false)
    private LocalDateTime fechaAdjunto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMovimientoId() { return movimientoId; }
    public void setMovimientoId(Long movimientoId) { this.movimientoId = movimientoId; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getRutaReferencia() { return rutaReferencia; }
    public void setRutaReferencia(String rutaReferencia) { this.rutaReferencia = rutaReferencia; }

    public String getTipoArchivo() { return tipoArchivo; }
    public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }

    public LocalDateTime getFechaAdjunto() { return fechaAdjunto; }
    public void setFechaAdjunto(LocalDateTime fechaAdjunto) { this.fechaAdjunto = fechaAdjunto; }

}

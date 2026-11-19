package cl.jdvzaror.reservas.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_reserva")
public class HistorialReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservaId;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoReserva estadoNuevo;

    @Column(nullable = false)
    private Long usuarioResponsable;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fechaCambio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public EstadoReserva getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoReserva estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public EstadoReserva getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoReserva estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public Long getUsuarioResponsable() { return usuarioResponsable; }
    public void setUsuarioResponsable(Long usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(LocalDateTime fechaCambio) { this.fechaCambio = fechaCambio; }

}

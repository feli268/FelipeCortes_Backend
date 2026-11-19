package cl.jdvzaror.finanzas.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_financiero")
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id")
    private CategoriaMovimiento categoria;

    private Long reservaId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Long monto;

    @Column(nullable = false)
    private LocalDate fechaMovimiento;

    @Column(nullable = false)
    private Long usuarioRegistroId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoMovimiento estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(unique = true, length = 64)
    private String claveCliente;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }

    public CategoriaMovimiento getCategoria() { return categoria; }
    public void setCategoria(CategoriaMovimiento categoria) { this.categoria = categoria; }

    public Long getReservaId() { return reservaId; }
    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }

    public LocalDate getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(LocalDate fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    public Long getUsuarioRegistroId() { return usuarioRegistroId; }
    public void setUsuarioRegistroId(Long usuarioRegistroId) { this.usuarioRegistroId = usuarioRegistroId; }

    public EstadoMovimiento getEstado() { return estado; }
    public void setEstado(EstadoMovimiento estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getClaveCliente() { return claveCliente; }
    public void setClaveCliente(String claveCliente) { this.claveCliente = claveCliente; }

}

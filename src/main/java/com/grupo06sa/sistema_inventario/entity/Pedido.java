package com.grupo06sa.sistema_inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado;

    @Column(name = "total")
    private Double total;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido")
    private List<DetallePedido> detallesPedido;

    @OneToMany(mappedBy = "pedido")
    private List<Pago> pagos;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<DetallePedido> getDetallesPedido() {
        return detallesPedido;
    }

    public void setDetallesPedido(List<DetallePedido> detallesPedido) {
        this.detallesPedido = detallesPedido;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }
}

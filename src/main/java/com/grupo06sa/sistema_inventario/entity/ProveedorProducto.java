package com.grupo06sa.sistema_inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "Proveedor_Producto",
    uniqueConstraints = @UniqueConstraint(columnNames = {"proveedor_id", "producto_id"})
)
public class ProveedorProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "costo_unitario_actual", nullable = false)
    private BigDecimal costoUnitarioActual;

    @Column(name = "costo_unitario_anterior")
    private BigDecimal costoUnitarioAnterior;

    @Column(name = "tiempo_reposicion_dias")
    private int tiempoReposicionDias = 0;

    @Column(name = "cantidad_minima_pedido")
    private BigDecimal cantidadMinimaPedido = BigDecimal.ONE;

    @Column(name = "disponible")
    private boolean disponible = true;

    @Column(name = "costo_actualizado_at")
    private LocalDateTime costoActualizadoAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getCostoUnitarioActual() {
        return costoUnitarioActual;
    }

    public void setCostoUnitarioActual(BigDecimal costoUnitarioActual) {
        this.costoUnitarioActual = costoUnitarioActual;
    }

    public BigDecimal getCostoUnitarioAnterior() {
        return costoUnitarioAnterior;
    }

    public void setCostoUnitarioAnterior(BigDecimal costoUnitarioAnterior) {
        this.costoUnitarioAnterior = costoUnitarioAnterior;
    }

    public int getTiempoReposicionDias() {
        return tiempoReposicionDias;
    }

    public void setTiempoReposicionDias(int tiempoReposicionDias) {
        this.tiempoReposicionDias = tiempoReposicionDias;
    }

    public BigDecimal getCantidadMinimaPedido() {
        return cantidadMinimaPedido;
    }

    public void setCantidadMinimaPedido(BigDecimal cantidadMinimaPedido) {
        this.cantidadMinimaPedido = cantidadMinimaPedido;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public LocalDateTime getCostoActualizadoAt() {
        return costoActualizadoAt;
    }

    public void setCostoActualizadoAt(LocalDateTime costoActualizadoAt) {
        this.costoActualizadoAt = costoActualizadoAt;
    }
}

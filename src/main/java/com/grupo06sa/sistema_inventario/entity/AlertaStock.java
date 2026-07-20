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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Alerta_Stock")
public class AlertaStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(name = "cantidad_actual")
    private BigDecimal cantidadActual;

    @Column(name = "stock_minimo")
    private BigDecimal stockMinimo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoAlerta estado = EstadoAlerta.PENDIENTE;

    @ManyToOne
    @JoinColumn(name = "atendida_por_id")
    private Usuario atendidaPor;

    @Column(name = "atendida_at")
    private LocalDateTime atendidaAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public void setAlmacen(Almacen almacen) {
        this.almacen = almacen;
    }

    public BigDecimal getCantidadActual() {
        return cantidadActual;
    }

    public void setCantidadActual(BigDecimal cantidadActual) {
        this.cantidadActual = cantidadActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public EstadoAlerta getEstado() {
        return estado;
    }

    public void setEstado(EstadoAlerta estado) {
        this.estado = estado;
    }

    public Usuario getAtendidaPor() {
        return atendidaPor;
    }

    public void setAtendidaPor(Usuario atendidaPor) {
        this.atendidaPor = atendidaPor;
    }

    public LocalDateTime getAtendidaAt() {
        return atendidaAt;
    }

    public void setAtendidaAt(LocalDateTime atendidaAt) {
        this.atendidaAt = atendidaAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

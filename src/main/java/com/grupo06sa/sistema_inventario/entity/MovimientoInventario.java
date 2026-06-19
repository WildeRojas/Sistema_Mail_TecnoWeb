package com.grupo06sa.sistema_inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "Movimiento_Inventario")
public class MovimientoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;

    @Column(name = "costo_unitario")
    private Double costoUnitario;

    @Column(name = "cantidad")
    private Integer cantidad;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @ManyToOne
    @JoinColumn(name = "tipo_operacion_id")
    private TipoOperacion tipoOperacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public Double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(Double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(TipoOperacion tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }
}

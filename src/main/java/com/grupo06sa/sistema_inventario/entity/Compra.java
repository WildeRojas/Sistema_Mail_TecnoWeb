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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Compra")
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudCompra solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoCompra estado = EstadoCompra.PENDIENTE;

    @Column(name = "total")
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "inventario_ingresado", nullable = false)
    private boolean inventarioIngresado = false;

    @OneToMany(mappedBy = "compra")
    private List<DetalleCompra> detalles;

    @OneToMany(mappedBy = "compra")
    private List<Pago> pagos;

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

    public Almacen getAlmacen() {
        return almacen;
    }

    public void setAlmacen(Almacen almacen) {
        this.almacen = almacen;
    }

    public SolicitudCompra getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudCompra solicitud) {
        this.solicitud = solicitud;
    }

    public EstadoCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoCompra estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public boolean isInventarioIngresado() {
        return inventarioIngresado;
    }

    public void setInventarioIngresado(boolean inventarioIngresado) {
        this.inventarioIngresado = inventarioIngresado;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }

    public BigDecimal totalPagado() {
        if (pagos == null || pagos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return pagos.stream()
            .filter(pago -> pago.getEstado() == EstadoPago.PAGADO)
            .map(Pago::getMonto)
            .filter(monto -> monto != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldo() {
        BigDecimal totalCompra = total != null ? total : BigDecimal.ZERO;
        BigDecimal restante = totalCompra.subtract(totalPagado());
        return restante.max(BigDecimal.ZERO);
    }
}

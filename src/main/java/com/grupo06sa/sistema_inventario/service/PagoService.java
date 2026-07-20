package com.grupo06sa.sistema_inventario.service;

import com.grupo06sa.sistema_inventario.entity.Compra;
import com.grupo06sa.sistema_inventario.entity.EstadoCompra;
import com.grupo06sa.sistema_inventario.entity.EstadoPago;
import com.grupo06sa.sistema_inventario.entity.Pago;
import com.grupo06sa.sistema_inventario.repository.CompraRepository;
import com.grupo06sa.sistema_inventario.repository.PagoRepository;
import com.grupo06sa.sistema_inventario.security.ContextoAutenticado;
import com.grupo06sa.sistema_inventario.security.RolNombre;
import com.grupo06sa.sistema_inventario.security.SecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {
    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final CompraRepository compraRepository;
    private final PagoFacilService pagoFacilService;
    private final SecurityService securityService;
    private final PagoService self;

    public PagoService(
        PagoRepository pagoRepository,
        CompraRepository compraRepository,
        PagoFacilService pagoFacilService,
        SecurityService securityService,
        @Lazy PagoService self
    ) {
        this.pagoRepository = pagoRepository;
        this.compraRepository = compraRepository;
        this.pagoFacilService = pagoFacilService;
        this.securityService = securityService;
        this.self = self;
    }

    @Transactional
    public Pago generarQrCompra(Long compraId) {
        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("La compra solicitada no existe."));

        List<Pago> pendientesPrevios = pagoRepository.findByCompraId(compraId).stream()
            .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
            .toList();
        pagoRepository.deleteAll(pendientesPrevios);

        BigDecimal monto = compra.saldo();
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La compra no tiene un monto pendiente de pago.");
        }

        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setMetodoPago("qr_pagofacil");
        pago.setMonto(monto);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setCreatedAt(LocalDateTime.now());
        Pago guardado = pagoRepository.save(pago);

        String referencia = "TJ" + compraId + "-" + guardado.getId();
        guardado.setReferencia(referencia);
        guardado = pagoRepository.save(guardado);

        String nombreProveedor = compra.getProveedor() != null ? compra.getProveedor().getNombre() : null;
        String correoProveedor = compra.getProveedor() != null ? compra.getProveedor().getCorreo() : null;
        String telefonoProveedor = compra.getProveedor() != null ? compra.getProveedor().getTelefono() : null;

        PagoFacilService.GeneracionQr generado = pagoFacilService.generarQr(
            nombreProveedor,
            null,
            telefonoProveedor,
            correoProveedor,
            referencia,
            "Pago compra #" + compraId,
            monto
        );

        guardado.setTransactionId(generado.transactionId());
        guardado.setVenceAt(generado.expiracion());
        guardado.setQrImagen(generado.qrBase64());
        return pagoRepository.save(guardado);
    }

    public Pago verificar(Long compraId, ContextoAutenticado ctx) {
        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("La compra solicitada no existe."));
        securityService.assertOwnership(compra.getProveedor().getId(), ctx);

        Pago pago = pagoRepository.findByCompraId(compraId).stream()
            .filter(p -> p.getEstado() == EstadoPago.PENDIENTE)
            .reduce((first, second) -> second)
            .orElseThrow(() -> new IllegalStateException("No hay un pago pendiente para esta compra."));

        PagoFacilService.EstadoPagoFacil estado =
            pagoFacilService.consultarEstado(pago.getTransactionId(), pago.getReferencia());

        if (estado.pagado()) {
            String datos = "paymentStatus=" + estado.paymentStatus() + ";origen=verificacion_activa";
            self.marcarPagadoPorReferencia(pago.getReferencia(), datos);
        }

        return pagoRepository.findByReferencia(pago.getReferencia()).orElse(pago);
    }

    @Transactional
    public void marcarPagadoPorReferencia(String referencia, String datosRespuestaSanitizado) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        Pago pago = pagoRepository.findWithLockByReferencia(referencia).orElse(null);
        aplicarPago(pago, datosRespuestaSanitizado);
    }

    @Transactional
    public void marcarPagadoPorTransactionId(String transactionId, String datosRespuestaSanitizado) {
        if (transactionId == null || transactionId.isBlank()) {
            return;
        }
        Pago pago = pagoRepository.findWithLockByTransactionId(transactionId).orElse(null);
        aplicarPago(pago, datosRespuestaSanitizado);
    }

    private void aplicarPago(Pago pago, String datosRespuestaSanitizado) {
        if (pago == null) {
            logger.warn("Notificación de pago recibida para una referencia o transacción inexistente.");
            return;
        }
        if (pago.getEstado() == EstadoPago.PAGADO) {
            logger.info("El pago con referencia {} ya estaba confirmado; se ignora la notificación duplicada.", pago.getReferencia());
            return;
        }

        pago.setEstado(EstadoPago.PAGADO);
        pago.setFechaPago(LocalDateTime.now());
        pago.setDatosRespuesta(datosRespuestaSanitizado);
        pagoRepository.save(pago);
        logger.info("Pago confirmado: referencia={}, compra={}", pago.getReferencia(), pago.getCompra().getId());

        Compra compra = compraRepository.findById(pago.getCompra().getId()).orElse(null);
        if (compra == null) {
            return;
        }
        if (compra.totalPagado().compareTo(compra.getTotal()) >= 0) {
            compra.setEstado(EstadoCompra.PAGADA);
            compraRepository.save(compra);
            logger.info("La compra {} pasó a estado PAGADA tras completarse el pago.", compra.getId());
        }
    }

    public List<Pago> listar(ContextoAutenticado ctx) {
        if (ctx.getRol() == RolNombre.PROVEEDOR) {
            return pagoRepository.findByCompraProveedorId(ctx.getProveedorId());
        }
        return pagoRepository.findAll();
    }
}

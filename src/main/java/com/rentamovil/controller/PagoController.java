package com.rentamovil.controller;

import com.rentamovil.dto.PagoRequest;
import com.rentamovil.dto.PagoResponse;
import com.rentamovil.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de pagos.
 * Todos los endpoints requieren rol ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    /** Registra un nuevo pago. Solo ADMIN. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody PagoRequest request) {
        log.info("Registrando pago para reserva ID: {}", request.getReservaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.crear(request));
    }

    /** Lista todos los pagos. Solo ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoResponse>> listar() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    /** Obtiene un pago por ID. Solo ADMIN. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    /** Lista pagos de una reserva. Solo ADMIN. */
    @GetMapping("/reserva/{reservaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoResponse>> listarPorReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(pagoService.listarPorReserva(reservaId));
    }

    /** Lista pagos de un cliente. Solo ADMIN. */
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PagoResponse>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(pagoService.listarPorCliente(clienteId));
    }

    /** Actualiza el estado de un pago. Solo ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PagoRequest request) {
        log.info("Actualizando pago ID: {}", id);
        return ResponseEntity.ok(pagoService.actualizar(id, request));
    }

    /** Elimina un pago. Solo ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando pago ID: {}", id);
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
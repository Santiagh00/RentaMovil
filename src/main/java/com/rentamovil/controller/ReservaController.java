package com.rentamovil.controller;

import com.rentamovil.dto.ReservaRequest;
import com.rentamovil.dto.ReservaResponse;
import com.rentamovil.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.List;

/**
 * Controlador REST para gestión de reservas.
 * - GET: usuarios autenticados
 * - POST/PUT/DELETE: solo ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    /** Crea una nueva reserva. Público (desde el portal del cliente). */
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request) {
        log.info("Creando reserva: cliente={}, vehículo={}", request.getClienteId(), request.getVehiculoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(request));
    }

    /** Lista todas las reservas. Solo ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listar() {
        return ResponseEntity.ok(reservaService.listarTodos());
    }

    /** Obtiene una reserva por ID. Solo ADMIN. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /** Lista reservas de un cliente específico. Solo ADMIN. */
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(reservaService.listarPorCliente(clienteId));
    }

    /** Lista reservas por estado. Solo ADMIN. */
    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(reservaService.listarPorEstado(estado));
    }

    /** Actualiza una reserva (estado, fechas, notas). Solo ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ReservaRequest request) {
        log.info("Actualizando reserva ID: {}", id);
        return ResponseEntity.ok(reservaService.actualizar(id, request));
    }

    /** Elimina una reserva. Solo ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando reserva ID: {}", id);
        reservaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Sube comprobante de pago. Público (desde la web del cliente). */
    @PostMapping("/{id}/comprobante")
    public ResponseEntity<ReservaResponse> subirComprobante(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("Subiendo comprobante para reserva ID: {}", id);
        return ResponseEntity.ok(reservaService.actualizarComprobante(id, file));
    }

    /** Valida el pago manualmente. Solo ADMIN. */
    @PutMapping("/{id}/validar-pago")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> validarPago(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        log.info("Admin validando pago para reserva ID: {}", id);
        boolean aprobado = payload.getOrDefault("aprobado", false);
        return ResponseEntity.ok(reservaService.validarPagoAdmin(id, aprobado));
    }
}
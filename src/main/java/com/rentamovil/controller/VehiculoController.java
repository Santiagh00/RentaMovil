package com.rentamovil.controller;

import com.rentamovil.dto.VehiculoRequest;
import com.rentamovil.dto.VehiculoResponse;
import com.rentamovil.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de vehículos.
 * - GET disponibles: acceso público (portal cliente)
 * - GET all/byId: requiere autenticación
 * - POST/PUT/DELETE: solo ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;

    /** Crea un nuevo vehículo en el sistema. Solo ADMIN. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest request) {
        log.info("Creando vehículo con placa: {}", request.getPlaca());
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculoService.crear(request));
    }

    /** Lista todos los vehículos. Requiere autenticación. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VehiculoResponse>> listar() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }

    /** Lista vehículos disponibles — accesible sin token (portal cliente). */
    @GetMapping("/disponibles")
    public ResponseEntity<List<VehiculoResponse>> listarDisponibles() {
        return ResponseEntity.ok(vehiculoService.listarPorEstado("disponible"));
    }

    /** Obtiene un vehículo por ID. Requiere autenticación. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VehiculoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculoService.obtenerPorId(id));
    }

    /** Lista vehículos por estado. Solo ADMIN. */
    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VehiculoResponse>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(vehiculoService.listarPorEstado(estado));
    }

    /** Actualiza un vehículo. Solo ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody VehiculoRequest request) {
        log.info("Actualizando vehículo ID: {}", id);
        return ResponseEntity.ok(vehiculoService.actualizar(id, request));
    }

    /** Elimina un vehículo. Solo ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando vehículo ID: {}", id);
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
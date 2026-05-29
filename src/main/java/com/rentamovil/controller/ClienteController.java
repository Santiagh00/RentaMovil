package com.rentamovil.controller;

import com.rentamovil.dto.ClienteRequest;
import com.rentamovil.dto.ClienteResponse;
import com.rentamovil.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de clientes.
 * - GET: cualquier usuario autenticado
 * - POST/PUT/DELETE: solo ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    /** Registra un nuevo cliente. Solo ADMIN. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        log.info("Registrando nuevo cliente con documento: {}", request.getNumeroDocumento());
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    /** Lista todos los clientes. Solo ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    /** Obtiene un cliente por ID. Solo ADMIN. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    /** Actualiza los datos de un cliente. Solo ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        log.info("Actualizando cliente ID: {}", id);
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    /** Elimina un cliente del sistema. Solo ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando cliente ID: {}", id);
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Busca un cliente por número de documento.
     * Solo ADMIN puede buscar clientes por documento.
     */
    @GetMapping("/documento/{numeroDocumento}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponse> obtenerPorNumeroDocumento(
            @PathVariable String numeroDocumento) {
        ClienteResponse response = clienteService.obtenerPorNumeroDocumento(numeroDocumento);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
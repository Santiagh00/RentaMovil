package com.rentamovil.controller;

import com.rentamovil.dto.PagoRequest;
import com.rentamovil.dto.PagoResponse;
import com.rentamovil.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> listar() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<List<PagoResponse>> listarPorReserva(@PathVariable Long reservaId) {
        return ResponseEntity.ok(pagoService.listarPorReserva(reservaId));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PagoResponse>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(pagoService.listarPorCliente(clienteId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(pagoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
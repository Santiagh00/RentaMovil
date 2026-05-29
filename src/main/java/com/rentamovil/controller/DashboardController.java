package com.rentamovil.controller;

import com.rentamovil.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador del dashboard de administración.
 * Retorna estadísticas agregadas del sistema.
 * Acceso exclusivo para ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // Protege toda la clase: todos los métodos requieren ADMIN
public class DashboardController {

    private final ClienteService clienteService;
    private final VehiculoService vehiculoService;
    private final ReservaService reservaService;

    /**
     * Retorna métricas clave del negocio para el panel de administración:
     * reservas activas, ingresos del mes, vehículos disponibles/rentados,
     * clientes activos y totales.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getEstadisticas() {
        log.debug("Generando estadísticas del dashboard");

        // LinkedHashMap para mantener el orden de las claves en la respuesta JSON
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("reservasActivas",    reservaService.countActivas());
        stats.put("ingresosMes",        reservaService.getIngresosMesActual());
        stats.put("vehiculosRentados",  vehiculoService.countRentados());
        stats.put("clientesActivos",    clienteService.countActivos());
        stats.put("vehiculosDisponibles", vehiculoService.countDisponibles());
        stats.put("totalVehiculos",     vehiculoService.listarTodos().size());
        stats.put("totalClientes",      clienteService.listarTodos().size());

        return ResponseEntity.ok(stats);
    }
}
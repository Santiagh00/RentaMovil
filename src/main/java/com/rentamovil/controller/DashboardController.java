package com.rentamovil.controller;

import com.rentamovil.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ClienteService clienteService;
    private final VehiculoService vehiculoService;
    private final ReservaService reservaService;
    private final PagoService pagoService;

    public DashboardController(ClienteService clienteService,
                              VehiculoService vehiculoService,
                              ReservaService reservaService,
                              PagoService pagoService) {
        this.clienteService = clienteService;
        this.vehiculoService = vehiculoService;
        this.reservaService = reservaService;
        this.pagoService = pagoService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getEstadisticas() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("reservasActivas", reservaService.countActivas());
        stats.put("ingresosMes", pagoService.getIngresosMesActual());
        stats.put("vehiculosRentados", vehiculoService.countRentados());
        stats.put("clientesActivos", clienteService.countActivos());
        stats.put("vehiculosDisponibles", vehiculoService.countDisponibles());
        stats.put("totalVehiculos", vehiculoService.listarTodos().size());
        stats.put("totalClientes", clienteService.listarTodos().size());

        return ResponseEntity.ok(stats);
    }
}
package com.rentamovil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReservaRequest {

    @NotNull(message = "El cliente es requerido")
    private Long clienteId;

    @NotNull(message = "El vehículo es requerido")
    private Long vehiculoId;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;

    private BigDecimal total;

    private String estado = "pendiente";

    private String notas;
}
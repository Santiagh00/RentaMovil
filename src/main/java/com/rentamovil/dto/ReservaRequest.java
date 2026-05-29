package com.rentamovil.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para crear o actualizar una reserva.
 */
@Data
public class ReservaRequest {

    @NotNull(message = "El ID del cliente es requerido")
    private Long clienteId;

    @NotNull(message = "El ID del vehículo es requerido")
    private Long vehiculoId;

    @NotNull(message = "La fecha de inicio es requerida")
    @FutureOrPresent(message = "La fecha de inicio debe ser hoy o una fecha futura")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    @FutureOrPresent(message = "La fecha de fin debe ser hoy o una fecha futura")
    private LocalDate fechaFin;

    // El total se calcula automáticamente en el servicio (días × precio/día)
    private BigDecimal total;

    private String estado = "pendiente";

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notas;

    private String comprobantePago;
}
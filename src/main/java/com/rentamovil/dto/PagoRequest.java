package com.rentamovil.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para registrar un pago.
 */
@Data
public class PagoRequest {

    @NotNull(message = "El ID del cliente es requerido")
    private Long clienteId;

    @NotNull(message = "El ID de la reserva es requerido")
    private Long reservaId;

    @NotBlank(message = "El método de pago es requerido")
    @Size(max = 50, message = "El método de pago no puede superar 50 caracteres")
    private String metodo;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotNull(message = "La fecha del pago es requerida")
    @PastOrPresent(message = "La fecha del pago no puede ser futura")
    private LocalDate fecha;

    private String estado = "pendiente";
}
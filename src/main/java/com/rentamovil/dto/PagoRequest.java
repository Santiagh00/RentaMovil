package com.rentamovil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagoRequest {

    @NotNull(message = "El cliente es requerido")
    private Long clienteId;

    @NotNull(message = "La reserva es requerida")
    private Long reservaId;

    @NotBlank(message = "El método de pago es requerido")
    private String metodo;

    @NotNull(message = "El monto es requerido")
    private BigDecimal monto;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    private String estado = "pendiente";
}
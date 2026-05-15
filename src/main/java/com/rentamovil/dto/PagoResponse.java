package com.rentamovil.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagoResponse {
    private Long id;
    private Long clienteId;
    private Long reservaId;
    private String metodo;
    private BigDecimal monto;
    private LocalDate fecha;
    private String estado;
}
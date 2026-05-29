package com.rentamovil.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReservaResponse {
    private Long id;
    private ClienteResponse cliente;
    private VehiculoResponse vehiculo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal total;
    private String estado;
    private String notas;
    private LocalDateTime fechaCreacion;
    private String comprobantePago;
}
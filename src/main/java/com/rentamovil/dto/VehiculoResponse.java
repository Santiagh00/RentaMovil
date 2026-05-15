package com.rentamovil.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VehiculoResponse {
    private Long id;
    private String tipo;
    private String marca;
    private String modelo;
    private Integer anio;
    private String placa;
    private String color;
    private Integer kilometraje;
    private BigDecimal precioDia;
    private String estado;
    private LocalDateTime fechaCreacion;
}
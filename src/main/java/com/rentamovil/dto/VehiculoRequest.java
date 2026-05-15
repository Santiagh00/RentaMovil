package com.rentamovil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehiculoRequest {

    @NotBlank(message = "El tipo es requerido")
    private String tipo;

    @NotBlank(message = "La marca es requerida")
    private String marca;

    @NotBlank(message = "El modelo es requerido")
    private String modelo;

    @NotNull(message = "El año es requerido")
    private Integer anio;

    @NotBlank(message = "La placa es requerida")
    private String placa;

    private String color;

    private Integer kilometraje = 0;

    @NotNull(message = "El precio por día es requerido")
    private BigDecimal precioDia;

    private String estado = "disponible";
}
package com.rentamovil.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar un vehículo.
 * Todas las validaciones se ejecutan antes de llegar al servicio.
 */
@Data
public class VehiculoRequest {

    @NotBlank(message = "El tipo de vehículo es requerido")
    @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
    private String tipo;

    @NotBlank(message = "La marca es requerida")
    @Size(max = 50, message = "La marca no puede superar 50 caracteres")
    private String marca;

    @NotBlank(message = "El modelo es requerido")
    @Size(max = 100, message = "El modelo no puede superar 100 caracteres")
    private String modelo;

    @NotNull(message = "El año es requerido")
    @Min(value = 1990, message = "El año debe ser 1990 o posterior")
    private Integer anio;

    @NotBlank(message = "La placa es requerida")
    @Size(min = 5, max = 10, message = "La placa debe tener entre 5 y 10 caracteres")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "La placa solo puede contener letras mayúsculas, números y guiones")
    private String placa;

    @Size(max = 30, message = "El color no puede superar 30 caracteres")
    private String color;

    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Integer kilometraje = 0;

    @NotNull(message = "El precio por día es requerido")
    @DecimalMin(value = "0.01", message = "El precio por día debe ser mayor a cero")
    private BigDecimal precioDia;

    private String estado = "disponible";
}
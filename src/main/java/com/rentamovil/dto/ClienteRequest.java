package com.rentamovil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "Los nombres son requeridos")
    private String nombres;

    @NotBlank(message = "Los apellidos son requeridos")
    private String apellidos;

    @NotBlank(message = "El tipo de documento es requerido")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es requerido")
    private String numeroDocumento;

    private String telefono;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;

    private String estado = "activo";
}
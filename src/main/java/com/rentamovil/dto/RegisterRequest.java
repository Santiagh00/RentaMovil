package com.rentamovil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de entrada para registrar un nuevo usuario administrador.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número")
    private String password;

    @NotBlank(message = "El nombre completo es requerido")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;
}

package com.rentamovil.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClienteResponse {
    private Long id;
    private String nombres;
    private String apellidos;
    private String tipoDocumento;
    private String numeroDocumento;
    private String telefono;
    private String email;
    private String estado;
    private LocalDateTime fechaCreacion;
}
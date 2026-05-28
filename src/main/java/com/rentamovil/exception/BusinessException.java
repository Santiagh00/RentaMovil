package com.rentamovil.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando se viola una regla de negocio (ej: email duplicado,
 * vehículo no disponible, fechas inválidas).
 * Spring la mapea a HTTP 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {

    private final String codigo;

    public BusinessException(String mensaje) {
        super(mensaje);
        this.codigo = "REGLA_NEGOCIO";
    }

    public BusinessException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}

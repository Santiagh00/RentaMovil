package com.rentamovil.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando un recurso solicitado no existe en la base de datos.
 * Spring la mapea automáticamente a HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String recurso;
    private final String campo;
    private final Object valor;

    public ResourceNotFoundException(String recurso, String campo, Object valor) {
        super(String.format("%s no encontrado con %s: '%s'", recurso, campo, valor));
        this.recurso = recurso;
        this.campo = campo;
        this.valor = valor;
    }

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
        this.recurso = null;
        this.campo = null;
        this.valor = null;
    }

    public String getRecurso() { return recurso; }
    public String getCampo() { return campo; }
    public Object getValor() { return valor; }
}

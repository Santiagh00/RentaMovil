package com.rentamovil.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para rutas web (no API).
 * Redirige la raíz a la página principal del cliente.
 */
@Controller
public class WebController {

    /**
     * Redirige la ruta raíz "/" a la página principal del cliente.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/UsuariosRentaMovil.html";
    }
}
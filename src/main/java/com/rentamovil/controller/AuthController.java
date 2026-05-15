package com.rentamovil.controller;

import com.rentamovil.dto.AuthRequest;
import com.rentamovil.dto.AuthResponse;
import com.rentamovil.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, "Usuario y contraseña requeridos"));
        }

        if (username.equals("admin") && password.equals("admin123")) {
            String token = jwtService.generarToken(username);
            return ResponseEntity.ok(new AuthResponse(token, "Login exitoso"));
        }

        return ResponseEntity.status(401).body(new AuthResponse(null, "Credenciales inválidas"));
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, "Token requerido"));
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extraerUsername(token);
            return ResponseEntity.ok(new AuthResponse(token, "Token válido para: " + username));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new AuthResponse(null, "Token inválido"));
        }
    }
}
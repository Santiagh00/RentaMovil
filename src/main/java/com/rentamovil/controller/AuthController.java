package com.rentamovil.controller;

import com.rentamovil.dto.AuthRequest;
import com.rentamovil.dto.AuthResponse;
import com.rentamovil.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación — rutas públicas (no requieren JWT).
 * Maneja login y validación de tokens.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Autentica un usuario con username y contraseña.
     * Spring Security verifica la contraseña con BCrypt contra la BD.
     *
     * @param request credenciales del usuario
     * @return JWT válido + mensaje de éxito
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Intento de login para usuario: {}", request.getUsername());

        // AuthenticationManager delega en DaoAuthenticationProvider:
        // 1. Carga el usuario via UserDetailsService
        // 2. Verifica la contraseña con BCryptPasswordEncoder
        // 3. Lanza BadCredentialsException si falla (manejado por GlobalExceptionHandler)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generarToken(userDetails);

        log.info("Login exitoso para usuario: {}", request.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, "Login exitoso"));
    }

    /**
     * Valida si un token JWT sigue siendo válido y no está expirado.
     * Útil para que el frontend verifique la sesión sin hacer un login completo.
     */
    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, "Token requerido en header Authorization"));
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtService.extraerUsername(token);
            String roles = jwtService.extraerRoles(token);
            return ResponseEntity.ok(
                    new AuthResponse(token, "Token válido — usuario: " + username + " | roles: " + roles));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Token inválido o expirado"));
        }
    }
}
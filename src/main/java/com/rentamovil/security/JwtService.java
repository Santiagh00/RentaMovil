package com.rentamovil.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Servicio responsable de generar, validar y parsear tokens JWT.
 * El token incluye el username como subject y el rol como claim personalizado,
 * lo que permite verificar permisos sin consultar la BD en cada request.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Genera un JWT firmado con HS256 que incluye:
     * - subject: username del usuario
     * - claim "roles": roles separados por coma (ej: "ROLE_ADMIN")
     * - issuedAt: momento de emisión
     * - expiration: momento de expiración
     *
     * @param userDetails el usuario autenticado de Spring Security
     * @return token JWT compacto y firmado
     */
    public String generarToken(UserDetails userDetails) {
        // Extraer todos los roles/authorities como cadena
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el username (subject) del token.
     * Lanza JwtException si el token es inválido o está expirado.
     */
    public String extraerUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    /**
     * Extrae los roles del claim "roles" del token.
     */
    public String extraerRoles(String token) {
        return (String) parsearClaims(token).get("roles");
    }

    /**
     * Verifica que el token corresponde al usuario dado y no está expirado.
     *
     * @param token       el JWT a validar
     * @param userDetails el usuario cargado desde la BD
     * @return true si el token es válido para este usuario
     */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            String usernameEnToken = extraerUsername(token);
            return usernameEnToken.equals(userDetails.getUsername())
                    && !estaExpirado(token)
                    && userDetails.isEnabled();
        } catch (JwtException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return false;
        }
    }

    // ─── Métodos privados ──────────────────────────────────────────────────────

    private boolean estaExpirado(String token) {
        return parsearClaims(token).getExpiration().before(new Date());
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Deriva la clave de firma HMAC-SHA a partir del secret configurado.
     * El secret debe tener al menos 32 caracteres para HS256.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
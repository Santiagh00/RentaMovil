package com.rentamovil.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que se ejecuta una sola vez por request (OncePerRequestFilter).
 * Intercepta cada petición HTTP, extrae el token Bearer del header Authorization,
 * lo valida y establece la autenticación en el SecurityContext de Spring.
 *
 * Flujo:
 *   Request → este filtro → verifica JWT → establece autenticación → sigue la cadena
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;  // Ahora usa el UserDetailsService real de BD

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Si no hay header Authorization o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.extraerUsername(token);

            // Solo autenticar si hay username y aún no hay autenticación activa en el contexto
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Cargar el usuario real desde la BD (con sus roles y estado activo)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.esTokenValido(token, userDetails)) {
                    // Construir el token de autenticación con los authorities del usuario
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // credentials null: no se necesitan tras autenticar
                                    userDetails.getAuthorities()   // Roles reales desde la BD
                            );
                    // Enriquecer con detalles del request (IP, session ID, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Establecer la autenticación en el contexto de seguridad del hilo actual
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario autenticado via JWT: {}", username);
                }
            }
        } catch (JwtException ex) {
            // Token malformado o expirado — dejar que Spring Security rechace el request (401)
            log.warn("JWT inválido en request a [{}]: {}", request.getRequestURI(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Error inesperado al procesar JWT: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
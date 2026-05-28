package com.rentamovil.config;

import com.rentamovil.service.UsuarioDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.rentamovil.security.JwtAuthFilter;

import java.util.List;

/**
 * Configuración central de seguridad de la aplicación.
 *
 * - CSRF deshabilitado: usamos JWT stateless, no sesiones de formulario.
 * - CORS configurado explícitamente para el frontend.
 * - Endpoints públicos: solo auth y recursos estáticos.
 * - Todo /api/** protegido: requiere JWT válido.
 * - @EnableMethodSecurity: activa @PreAuthorize/@PostAuthorize en controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // Habilita @PreAuthorize y @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UsuarioDetailsServiceImpl userDetailsService;

    // ─── Cadena de filtros principal ──────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF innecesario con JWT stateless
            .csrf(AbstractHttpConfigurer::disable)

            // CORS usando nuestra configuración explícita
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Reglas de autorización por ruta
            .authorizeHttpRequests(auth -> auth
                // ── Rutas públicas ──────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(
                    "/", "/index.html", "/admin.html",
                    "/css/**", "/js/**", "/assets/**", "/favicon.ico"
                ).permitAll()
                // Consulta de vehículos disponibles accesible sin login (portal cliente)
                .requestMatchers(HttpMethod.GET, "/api/vehiculos/disponibles").permitAll()

                // ── Rutas protegidas: requieren JWT válido ──────────────────
                .anyRequest().authenticated()
            )

            // Sin sesiones HTTP — cada request es autónomo con su JWT
            .sessionManagement(sess -> sess
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Proveedor de autenticación con UserDetailsService + BCrypt
            .authenticationProvider(authenticationProvider())

            // Nuestro filtro JWT corre antes que el filtro estándar de usuario/contraseña
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─── Beans de autenticación ────────────────────────────────────────────────

    /**
     * BCrypt con strength 12 (recomendado para producción).
     * Strength 10 es el default; 12 añade ~4x más tiempo de cómputo para ataques.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Proveedor DAO: conecta UserDetailsService + PasswordEncoder.
     * Spring Security lo usa para autenticar usuario/contraseña.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager necesario para que AuthController pueda
     * delegar la autenticación manualmente.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ─── Configuración CORS ────────────────────────────────────────────────────

    /**
     * CORS explícito: permite que el frontend (mismo servidor, puerto 8080)
     * y cualquier origen local consuma la API.
     * En producción reemplazar "*" por el dominio real del frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);   // Cachear preflight 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
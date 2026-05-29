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
    private final RateLimitFilter rateLimitFilter;
    private final UsuarioDetailsServiceImpl userDetailsService;

    // ─── Cadena de filtros principal ──────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF innecesario con JWT stateless
            .csrf(AbstractHttpConfigurer::disable)

            // CORS usando nuestra configuración explícita
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Permisos para H2 Console (frames)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            // Reglas de autorización por ruta
            .authorizeHttpRequests(auth -> auth
                // ── Rutas públicas ──────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(
                    "/", "/UsuariosRentaMovil.html", "/admin.html",
                    "/css/**", "/js/**", "/assets/**", "/favicon.ico"
                ).permitAll()
                // H2 Console
                .requestMatchers("/h2-console/**").permitAll()
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
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Rate limiting antes de todo para bloquear ataques rápidamente
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

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
     * CORS configurado para desarrollo.
     * PRECAUCIÓN: En producción, restringir allowedOrigins al dominio del frontend.
     * Usar variable de entorno CORS_ALLOWED_ORIGINS para configurar orígenes.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String corsOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (corsOrigins != null && !corsOrigins.isBlank()) {
            config.setAllowedOrigins(List.of(corsOrigins.split(",")));
        } else {
            config.setAllowedOrigins(List.of("http://localhost:8080", "http://localhost:8081", "http://localhost:5173"));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
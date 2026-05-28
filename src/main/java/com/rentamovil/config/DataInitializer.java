package com.rentamovil.config;

import com.rentamovil.model.Rol;
import com.rentamovil.model.Usuario;
import com.rentamovil.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicializador de datos del sistema.
 * Se ejecuta automáticamente al arrancar la aplicación.
 * Crea el usuario administrador por defecto si no existe,
 * garantizando que siempre haya al menos un admin para hacer login.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdminUsuario() {
        return args -> {
            // Solo crear si no existe, evitando duplicados en cada reinicio
            if (!usuarioRepository.existsByUsername("admin")) {
                Usuario admin = Usuario.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .nombre("Administrador del Sistema")
                        .rol(Rol.ADMIN)
                        .activo(true)
                        .build();

                usuarioRepository.save(admin);
                log.info("✅ Usuario administrador creado: admin / admin123");
                log.warn("⚠️  Cambia la contraseña por defecto en producción");
            } else {
                log.info("✅ Usuario admin ya existe en la base de datos");
            }
        };
    }
}

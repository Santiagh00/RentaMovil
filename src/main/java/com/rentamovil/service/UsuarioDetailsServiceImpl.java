package com.rentamovil.service;

import com.rentamovil.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del contrato UserDetailsService de Spring Security.
 * Spring Security llama a este servicio durante el proceso de autenticación
 * para cargar el usuario desde la base de datos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga el usuario por username. Spring Security lo usa para:
     * 1. Verificar la contraseña con BCryptPasswordEncoder.
     * 2. Cargar los roles/authorities del usuario.
     *
     * @param username el nombre de usuario
     * @return UserDetails con la info del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Cargando usuario desde BD: {}", username);

        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Intento de login con username no registrado: {}", username);
                    return new UsernameNotFoundException(
                            "Usuario no encontrado: " + username);
                });
    }
}

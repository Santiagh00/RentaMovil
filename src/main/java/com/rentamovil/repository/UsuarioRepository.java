package com.rentamovil.repository;

import com.rentamovil.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Usuario.
 * Spring Data genera automáticamente las implementaciones de los métodos.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca un usuario por username para autenticación. */
    Optional<Usuario> findByUsername(String username);

    /** Verifica si existe un username antes de registrarlo. */
    boolean existsByUsername(String username);
}

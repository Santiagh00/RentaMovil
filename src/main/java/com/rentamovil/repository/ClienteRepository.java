package com.rentamovil.repository;

import com.rentamovil.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    boolean existsByEmail(String email);
    boolean existsByNumeroDocumento(String numeroDocumento);
}
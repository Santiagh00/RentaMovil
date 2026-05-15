package com.rentamovil.repository;

import com.rentamovil.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByClienteId(Long clienteId);
    List<Pago> findByReservaId(Long reservaId);
    List<Pago> findByEstado(String estado);
}
package com.rentamovil.repository;

import com.rentamovil.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByClienteId(Long clienteId);
    List<Reserva> findByVehiculoId(Long vehiculoId);
    List<Reserva> findByEstado(String estado);
}
package com.rentamovil.repository;

import com.rentamovil.model.Reserva;
import com.rentamovil.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByClienteId(Long clienteId);
    List<Reserva> findByEstado(EstadoReserva estado);

    // Consulta nativa/JPQL para verificar si el vehículo ya está ocupado en esas fechas exactas
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.vehiculo.id = :vehiculoId " +
            "AND r.estado IN (com.rentamovil.model.enums.EstadoReserva.PENDIENTE, com.rentamovil.model.enums.EstadoReserva.ACTIVA) " +
            "AND (:fechaInicio <= r.fechaFin AND :fechaFin >= r.fechaInicio)")
    boolean verificarCruzeDeFechas(
            @Param("vehiculoId") Long vehiculoId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin
    );
}
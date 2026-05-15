package com.rentamovil.repository;

import com.rentamovil.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPlaca(String placa);
    List<Vehiculo> findByEstado(String estado);
    List<Vehiculo> findByTipo(String tipo);
    boolean existsByPlaca(String placa);
}
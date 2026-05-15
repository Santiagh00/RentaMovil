package com.rentamovil.service;

import com.rentamovil.dto.VehiculoRequest;
import com.rentamovil.dto.VehiculoResponse;
import com.rentamovil.model.Vehiculo;
import com.rentamovil.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional
    public VehiculoResponse crear(VehiculoRequest request) {
        if (vehiculoRepository.existsByPlaca(request.getPlaca())) {
            throw new RuntimeException("La placa ya está registrada");
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(request.getTipo());
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setPlaca(request.getPlaca());
        vehiculo.setColor(request.getColor());
        vehiculo.setKilometraje(request.getKilometraje() != null ? request.getKilometraje() : 0);
        vehiculo.setPrecioDia(request.getPrecioDia());
        vehiculo.setEstado(request.getEstado() != null ? request.getEstado() : "disponible");

        return mapToResponse(vehiculoRepository.save(vehiculo));
    }

    public List<VehiculoResponse> listarTodos() {
        return vehiculoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VehiculoResponse obtenerPorId(Long id) {
        return vehiculoRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con id: " + id));
    }

    public List<VehiculoResponse> listarPorEstado(String estado) {
        return vehiculoRepository.findByEstado(estado).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehiculoResponse actualizar(Long id, VehiculoRequest request) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con id: " + id));

        if (!vehiculo.getPlaca().equals(request.getPlaca())
                && vehiculoRepository.existsByPlaca(request.getPlaca())) {
            throw new RuntimeException("La placa ya está registrada");
        }

        vehiculo.setTipo(request.getTipo());
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setPlaca(request.getPlaca());
        vehiculo.setColor(request.getColor());
        if (request.getKilometraje() != null) {
            vehiculo.setKilometraje(request.getKilometraje());
        }
        vehiculo.setPrecioDia(request.getPrecioDia());
        if (request.getEstado() != null) {
            vehiculo.setEstado(request.getEstado());
        }

        return mapToResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!vehiculoRepository.existsById(id)) {
            throw new RuntimeException("Vehículo no encontrado con id: " + id);
        }
        vehiculoRepository.deleteById(id);
    }

    public long countDisponibles() {
        return vehiculoRepository.findByEstado("disponible").size();
    }

    public long countRentados() {
        return vehiculoRepository.findByEstado("rentado").size();
    }

    private VehiculoResponse mapToResponse(Vehiculo vehiculo) {
        VehiculoResponse response = new VehiculoResponse();
        response.setId(vehiculo.getId());
        response.setTipo(vehiculo.getTipo());
        response.setMarca(vehiculo.getMarca());
        response.setModelo(vehiculo.getModelo());
        response.setAnio(vehiculo.getAnio());
        response.setPlaca(vehiculo.getPlaca());
        response.setColor(vehiculo.getColor());
        response.setKilometraje(vehiculo.getKilometraje());
        response.setPrecioDia(vehiculo.getPrecioDia());
        response.setEstado(vehiculo.getEstado());
        response.setFechaCreacion(vehiculo.getFechaCreacion());
        return response;
    }
}
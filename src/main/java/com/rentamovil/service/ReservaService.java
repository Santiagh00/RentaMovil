package com.rentamovil.service;

import com.rentamovil.dto.ReservaRequest;
import com.rentamovil.dto.ReservaResponse;
import com.rentamovil.exception.BusinessException;
import com.rentamovil.exception.ResourceNotFoundException;
import com.rentamovil.model.Cliente;
import com.rentamovil.model.Reserva;
import com.rentamovil.model.Vehiculo;
import com.rentamovil.repository.ClienteRepository;
import com.rentamovil.repository.ReservaRepository;
import com.rentamovil.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para reservas.
 * Gestiona el ciclo de vida completo: creación, actualización de estado
 * y liberación automática del vehículo al completar/cancelar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    @Transactional
    public ReservaResponse crear(ReservaRequest request) {
        log.info("Creando reserva: cliente={}, vehículo={}", request.getClienteId(), request.getVehiculoId());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.getClienteId()));

        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo", "id", request.getVehiculoId()));

        if (!"disponible".equals(vehiculo.getEstado())) {
            throw new BusinessException("VEHICULO_NO_DISPONIBLE",
                    "El vehículo con placa '" + vehiculo.getPlaca() + "' no está disponible");
        }

        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new BusinessException("FECHAS_INVALIDAS",
                    "La fecha de inicio no puede ser posterior a la fecha fin");
        }

        if (request.getFechaInicio().isBefore(LocalDate.now())) {
            throw new BusinessException("FECHA_PASADA",
                    "La fecha de inicio no puede ser anterior a hoy");
        }

        long dias = ChronoUnit.DAYS.between(request.getFechaInicio(), request.getFechaFin());
        if (dias < 1) {
            throw new BusinessException("DURACION_INVALIDA",
                    "La reserva debe ser de al menos 1 día");
        }

        BigDecimal total = vehiculo.getPrecioDia().multiply(BigDecimal.valueOf(dias));

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaInicio(request.getFechaInicio());
        reserva.setFechaFin(request.getFechaFin());
        reserva.setTotal(total);
        reserva.setEstado(request.getEstado() != null ? request.getEstado() : "pendiente");
        reserva.setNotas(request.getNotas());

        // Marcar el vehículo como rentado para evitar doble reserva
        vehiculo.setEstado("rentado");
        vehiculoRepository.save(vehiculo);

        Reserva guardada = reservaRepository.save(reserva);
        log.info("Reserva creada con ID: {}, total: {}", guardada.getId(), total);
        return mapToResponse(guardada);
    }

    public List<ReservaResponse> listarTodos() {
        return reservaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ReservaResponse obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
    }

    public List<ReservaResponse> listarPorCliente(Long clienteId) {
        return reservaRepository.findByClienteId(clienteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReservaResponse> listarPorEstado(String estado) {
        return reservaRepository.findByEstado(estado).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservaResponse actualizar(Long id, ReservaRequest request) {
        log.info("Actualizando reserva ID: {}", id);

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        if (request.getFechaInicio() != null && request.getFechaFin() != null) {
            if (request.getFechaInicio().isAfter(request.getFechaFin())) {
                throw new BusinessException("FECHAS_INVALIDAS",
                        "La fecha de inicio no puede ser posterior a la fecha fin");
            }
            reserva.setFechaInicio(request.getFechaInicio());
            reserva.setFechaFin(request.getFechaFin());

            long dias = ChronoUnit.DAYS.between(reserva.getFechaInicio(), reserva.getFechaFin());
            BigDecimal total = reserva.getVehiculo().getPrecioDia().multiply(BigDecimal.valueOf(dias));
            reserva.setTotal(total);
        }

        if (request.getEstado() != null) {
            reserva.setEstado(request.getEstado());

            // Al completar o cancelar, liberar el vehículo automáticamente
            if ("completada".equals(request.getEstado()) || "cancelada".equals(request.getEstado())) {
                Vehiculo vehiculo = reserva.getVehiculo();
                vehiculo.setEstado("disponible");
                vehiculoRepository.save(vehiculo);
                log.info("Vehículo {} liberado por reserva {} ({})",
                        vehiculo.getPlaca(), id, request.getEstado());
            }
        }

        if (request.getNotas() != null) {
            reserva.setNotas(request.getNotas());
        }

        return mapToResponse(reservaRepository.save(reserva));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando reserva ID: {}", id);

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        // Liberar vehículo si la reserva estaba activa o pendiente
        if ("activa".equals(reserva.getEstado()) || "pendiente".equals(reserva.getEstado())) {
            Vehiculo vehiculo = reserva.getVehiculo();
            vehiculo.setEstado("disponible");
            vehiculoRepository.save(vehiculo);
        }

        reservaRepository.deleteById(id);
    }

    public long countActivas() {
        return reservaRepository.findByEstado("activa").size() +
               reservaRepository.findByEstado("pendiente").size();
    }

    // ─── Mappers internos ──────────────────────────────────────────────────────

    private ReservaResponse mapToResponse(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setId(reserva.getId());
        response.setCliente(mapClienteToResponse(reserva.getCliente()));
        response.setVehiculo(mapVehiculoToResponse(reserva.getVehiculo()));
        response.setFechaInicio(reserva.getFechaInicio());
        response.setFechaFin(reserva.getFechaFin());
        response.setTotal(reserva.getTotal());
        response.setEstado(reserva.getEstado());
        response.setNotas(reserva.getNotas());
        response.setFechaCreacion(reserva.getFechaCreacion());
        return response;
    }

    private com.rentamovil.dto.ClienteResponse mapClienteToResponse(Cliente cliente) {
        com.rentamovil.dto.ClienteResponse r = new com.rentamovil.dto.ClienteResponse();
        r.setId(cliente.getId());
        r.setNombres(cliente.getNombres());
        r.setApellidos(cliente.getApellidos());
        r.setTipoDocumento(cliente.getTipoDocumento());
        r.setNumeroDocumento(cliente.getNumeroDocumento());
        r.setTelefono(cliente.getTelefono());
        r.setEmail(cliente.getEmail());
        r.setEstado(cliente.getEstado());
        return r;
    }

    private com.rentamovil.dto.VehiculoResponse mapVehiculoToResponse(Vehiculo vehiculo) {
        com.rentamovil.dto.VehiculoResponse r = new com.rentamovil.dto.VehiculoResponse();
        r.setId(vehiculo.getId());
        r.setTipo(vehiculo.getTipo());
        r.setMarca(vehiculo.getMarca());
        r.setModelo(vehiculo.getModelo());
        r.setAnio(vehiculo.getAnio());
        r.setPlaca(vehiculo.getPlaca());
        r.setColor(vehiculo.getColor());
        r.setKilometraje(vehiculo.getKilometraje());
        r.setPrecioDia(vehiculo.getPrecioDia());
        r.setEstado(vehiculo.getEstado());
        return r;
    }
}
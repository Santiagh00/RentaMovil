package com.rentamovil.service;

import com.rentamovil.dto.PagoRequest;
import com.rentamovil.dto.PagoResponse;
import com.rentamovil.exception.BusinessException;
import com.rentamovil.exception.ResourceNotFoundException;
import com.rentamovil.model.Cliente;
import com.rentamovil.model.Pago;
import com.rentamovil.model.Reserva;
import com.rentamovil.repository.ClienteRepository;
import com.rentamovil.repository.PagoRepository;
import com.rentamovil.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para pagos.
 * Valida que el pago corresponda a la reserva y cliente correctos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    @Transactional
    public PagoResponse crear(PagoRequest request) {
        log.info("Registrando pago para reserva ID: {}", request.getReservaId());

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.getClienteId()));

        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", request.getReservaId()));

        // Validar que la reserva pertenece al cliente indicado
        if (!reserva.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("RESERVA_CLIENTE_MISMATCH",
                    "La reserva no pertenece al cliente especificado");
        }

        if (request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("MONTO_INVALIDO",
                    "El monto del pago debe ser mayor a cero");
        }

        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setReserva(reserva);
        pago.setMetodo(request.getMetodo());
        pago.setMonto(request.getMonto());
        pago.setFecha(request.getFecha());
        pago.setEstado(request.getEstado() != null ? request.getEstado() : "pendiente");

        Pago guardado = pagoRepository.save(pago);
        log.info("Pago registrado con ID: {}, monto: {}", guardado.getId(), request.getMonto());
        return mapToResponse(guardado);
    }

    public List<PagoResponse> listarTodos() {
        return pagoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PagoResponse obtenerPorId(Long id) {
        return pagoRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", id));
    }

    public List<PagoResponse> listarPorReserva(Long reservaId) {
        return pagoRepository.findByReservaId(reservaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PagoResponse> listarPorCliente(Long clienteId) {
        return pagoRepository.findByClienteId(clienteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PagoResponse actualizar(Long id, PagoRequest request) {
        log.info("Actualizando pago ID: {}", id);

        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", id));

        if (request.getMetodo() != null)  pago.setMetodo(request.getMetodo());
        if (request.getMonto() != null)   pago.setMonto(request.getMonto());
        if (request.getFecha() != null)   pago.setFecha(request.getFecha());
        if (request.getEstado() != null)  pago.setEstado(request.getEstado());

        return mapToResponse(pagoRepository.save(pago));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando pago ID: {}", id);
        if (!pagoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pago", "id", id);
        }
        pagoRepository.deleteById(id);
    }

    /**
     * Calcula los ingresos del mes actual sumando pagos con estado "completado".
     */
    public BigDecimal getIngresosMesActual() {
        LocalDate now = LocalDate.now();
        LocalDate inicioMes = now.withDayOfMonth(1);
        LocalDate finMes = now.withDayOfMonth(now.lengthOfMonth());

        return pagoRepository.findAll().stream()
                .filter(p -> !p.getFecha().isBefore(inicioMes) && !p.getFecha().isAfter(finMes))
                .filter(p -> "completado".equals(p.getEstado()))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── Mapper interno ────────────────────────────────────────────────────────

    private PagoResponse mapToResponse(Pago pago) {
        PagoResponse response = new PagoResponse();
        response.setId(pago.getId());
        response.setClienteId(pago.getCliente().getId());
        response.setReservaId(pago.getReserva().getId());
        response.setMetodo(pago.getMetodo());
        response.setMonto(pago.getMonto());
        response.setFecha(pago.getFecha());
        response.setEstado(pago.getEstado());
        return response;
    }
}
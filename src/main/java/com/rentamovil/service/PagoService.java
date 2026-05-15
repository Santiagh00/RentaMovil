package com.rentamovil.service;

import com.rentamovil.dto.PagoRequest;
import com.rentamovil.dto.PagoResponse;
import com.rentamovil.model.Cliente;
import com.rentamovil.model.Pago;
import com.rentamovil.model.Reserva;
import com.rentamovil.repository.ClienteRepository;
import com.rentamovil.repository.PagoRepository;
import com.rentamovil.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    public PagoService(PagoRepository pagoRepository,
                      ClienteRepository clienteRepository,
                      ReservaRepository reservaRepository) {
        this.pagoRepository = pagoRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public PagoResponse crear(PagoRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Reserva reserva = reservaRepository.findById(request.getReservaId())
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reserva.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("La reserva no pertenece al cliente especificado");
        }

        Pago pago = new Pago();
        pago.setCliente(cliente);
        pago.setReserva(reserva);
        pago.setMetodo(request.getMetodo());
        pago.setMonto(request.getMonto());
        pago.setFecha(request.getFecha());
        pago.setEstado(request.getEstado() != null ? request.getEstado() : "pendiente");

        return mapToResponse(pagoRepository.save(pago));
    }

    public List<PagoResponse> listarTodos() {
        return pagoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PagoResponse obtenerPorId(Long id) {
        return pagoRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + id));
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
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        if (request.getMetodo() != null) {
            pago.setMetodo(request.getMetodo());
        }
        if (request.getMonto() != null) {
            pago.setMonto(request.getMonto());
        }
        if (request.getFecha() != null) {
            pago.setFecha(request.getFecha());
        }
        if (request.getEstado() != null) {
            pago.setEstado(request.getEstado());
        }

        return mapToResponse(pagoRepository.save(pago));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!pagoRepository.existsById(id)) {
            throw new RuntimeException("Pago no encontrado con id: " + id);
        }
        pagoRepository.deleteById(id);
    }

    public BigDecimal getIngresosMesActual() {
        LocalDate now = LocalDate.now();
        LocalDate inicioMes = now.withDayOfMonth(1);
        LocalDate finMes = now.withDayOfMonth(now.lengthOfMonth());

        return pagoRepository.findAll().stream()
                .filter(p -> p.getFecha().isAfter(inicioMes.minusDays(1))
                          && p.getFecha().isBefore(finMes.plusDays(1)))
                .filter(p -> "completado".equals(p.getEstado()))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

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
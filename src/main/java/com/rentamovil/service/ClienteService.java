package com.rentamovil.service;

import com.rentamovil.dto.ClienteRequest;
import com.rentamovil.dto.ClienteResponse;
import com.rentamovil.exception.BusinessException;
import com.rentamovil.exception.ResourceNotFoundException;
import com.rentamovil.model.Cliente;
import com.rentamovil.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para clientes.
 * - Las lecturas usan @Transactional(readOnly=true): optimización JPA que
 *   evita flush y permite al ORM hacer optimizaciones de caché.
 * - Las escrituras usan @Transactional por defecto (readOnly=false).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        log.info("Creando cliente con documento: {}", request.getNumeroDocumento());

        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_DUPLICADO",
                    "El email '" + request.getEmail() + "' ya está registrado");
        }
        if (clienteRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new BusinessException("DOCUMENTO_DUPLICADO",
                    "El número de documento '" + request.getNumeroDocumento() + "' ya está registrado");
        }

        Cliente cliente = new Cliente();
        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNumeroDocumento(request.getNumeroDocumento());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setEstado(request.getEstado() != null ? request.getEstado() : "activo");

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente creado con ID: {}", guardado.getId());
        return mapToResponse(guardado);
    }

    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ClienteResponse obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        log.info("Actualizando cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));

        // Verificar email solo si cambió
        if (!cliente.getEmail().equals(request.getEmail())
                && clienteRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_DUPLICADO",
                    "El email '" + request.getEmail() + "' ya está registrado");
        }

        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNumeroDocumento(request.getNumeroDocumento());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        if (request.getEstado() != null) {
            cliente.setEstado(request.getEstado());
        }

        return mapToResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando cliente ID: {}", id);
        if (!clienteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente", "id", id);
        }
        clienteRepository.deleteById(id);
    }

    public long countActivos() {
        return clienteRepository.findAll().stream()
                .filter(c -> "activo".equals(c.getEstado()))
                .count();
    }

    public ClienteResponse obtenerPorNumeroDocumento(String numeroDocumento) {
        return clienteRepository.findByNumeroDocumento(numeroDocumento)
                .map(this::mapToResponse)
                .orElse(null);
    }

    // ─── Mapper interno ────────────────────────────────────────────────────────

    private ClienteResponse mapToResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setNombres(cliente.getNombres());
        response.setApellidos(cliente.getApellidos());
        response.setTipoDocumento(cliente.getTipoDocumento());
        response.setNumeroDocumento(cliente.getNumeroDocumento());
        response.setTelefono(cliente.getTelefono());
        response.setEmail(cliente.getEmail());
        response.setEstado(cliente.getEstado());
        response.setFechaCreacion(cliente.getFechaCreacion());
        return response;
    }
}
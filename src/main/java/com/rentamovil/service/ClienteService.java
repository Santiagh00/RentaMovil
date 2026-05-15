package com.rentamovil.service;

import com.rentamovil.dto.ClienteRequest;
import com.rentamovil.dto.ClienteResponse;
import com.rentamovil.model.Cliente;
import com.rentamovil.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        if (clienteRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new RuntimeException("El número de documento ya está registrado");
        }

        Cliente cliente = new Cliente();
        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNumeroDocumento(request.getNumeroDocumento());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setEstado(request.getEstado() != null ? request.getEstado() : "activo");

        return mapToResponse(clienteRepository.save(cliente));
    }

    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ClienteResponse obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));

        if (!cliente.getEmail().equals(request.getEmail())
                && clienteRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
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
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con id: " + id);
        }
        clienteRepository.deleteById(id);
    }

    public long countActivos() {
        return clienteRepository.findAll().stream()
                .filter(c -> "activo".equals(c.getEstado()))
                .count();
    }

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
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
 * SERVICIO DE LÓGICA DE NEGOCIO PARA RESERVAS.
 * GESTIONA EL CICLO DE VIDA COMPLETO: CREACIÓN, ACTUALIZACIÓN DE ESTADO
 * Y LIBERACIÓN AUTOMÁTICA DEL VEHÍCULO AL COMPLETAR/CANCELAR.
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

        // BUSCAR CLIENTE EN LA BASE DE DATOS O LANZAR EXCEPCIÓN SI NO EXISTE
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.getClienteId()));

        // BUSCAR VEHÍCULO EN LA BASE DE DATOS O LANZAR EXCEPCIÓN SI NO EXISTE
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo", "id", request.getVehiculoId()));

        // VALIDACIÓN 1: SI EL VEHÍCULO ESTÁ EN EL TALLER, NO SE PUEDE RENTAR BAJO NINGÚN TÉRMINO
        if (com.rentamovil.model.enums.EstadoVehiculo.MANTENIMIENTO.equals(vehiculo.getEstado())) {
            throw new BusinessException("VEHICULO_EN_MANTENIMIENTO",
                    "El vehículo con placa '" + vehiculo.getPlaca() + "' está en mantenimiento técnico.");
        }

        // VALIDACIÓN 2: VERIFICAR QUE LA FECHA DE INICIO NO SEA POSTERIOR A LA FECHA FIN
        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new BusinessException("FECHAS_INVALIDAS",
                    "La fecha de inicio no puede ser posterior a la fecha fin");
        }

        // VALIDACIÓN 3: VERIFICAR QUE LA FECHA DE INICIO NO SEA ANTERIOR A HOY
        if (request.getFechaInicio().isBefore(LocalDate.now())) {
            throw new BusinessException("FECHA_PASADA",
                    "La fecha de inicio no puede ser anterior a hoy");
        }

        // VALIDACIÓN 4: CALCULAR LOS DÍAS DE RENTA Y COMPROBAR QUE SEA MÍNIMO 1 DÍA
        long dias = ChronoUnit.DAYS.between(request.getFechaInicio(), request.getFechaFin());
        if (dias < 1) {
            throw new BusinessException("DURACION_INVALIDA",
                    "La reserva debe ser de al menos 1 día");
        }

        // VALIDACIÓN 5: CORRECCIÓN DEL BUG DE DOBLE RESERVA MEDIANTE RANGOS DE FECHAS
        boolean estaOcupado = reservaRepository.verificarCruzeDeFechas(vehiculo.getId(), request.getFechaInicio(), request.getFechaFin());
        if (estaOcupado) {
            throw new BusinessException("VEHICULO_NO_DISPONIBLE",
                    "El vehículo ya cuenta con una reserva activa o pendiente en el rango de fechas seleccionado.");
        }

        // CALCULAR EL COSTO TOTAL DE LA RENTA (DÍAS MULTIPLICADO POR PRECIO POR DIARIO)
        BigDecimal total = vehiculo.getPrecioDia().multiply(BigDecimal.valueOf(dias));

        // CREAR LA INSTANCIA DE RESERVA CON LOS DATOS SUMINISTRADOS
        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaInicio(request.getFechaInicio());
        reserva.setFechaFin(request.getFechaFin());
        reserva.setTotal(total);

        // CONVERSIÓN SEGURA DEL STRING DEL REQUEST AL ENUM CORRESPONDIENTE
        reserva.setEstado(request.getEstado() != null ?
                com.rentamovil.model.enums.EstadoReserva.valueOf(request.getEstado().toUpperCase()) :
                com.rentamovil.model.enums.EstadoReserva.PENDIENTE);

        reserva.setNotas(request.getNotas());

        // EL VEHÍCULO SE MANTIENE DISPONIBLE PARA OTRAS FECHAS, SOLO CAMBIA SI LA RESERVA EMPIEZA HOY
        if (request.getFechaInicio().isEqual(LocalDate.now())) {
            vehiculo.setEstado(com.rentamovil.model.enums.EstadoVehiculo.RENTADO);
            vehiculoRepository.save(vehiculo);
        }

        // GUARDAR LA RESERVA REGISTRADA EN LA BASE DE DATOS
        Reserva guardada = reservaRepository.save(reserva);
        log.info("Reserva creada con ID: {}, total: {}", guardada.getId(), total);
        return mapToResponse(guardada);
    }

    // OBTENER LA LISTA COMPLETA DE RESERVAS EXISTENTES
    public List<ReservaResponse> listarTodos() {
        return reservaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // BUSCAR UNA RESERVA ESPECÍFICA MEDIANTE SU IDENTIFICADOR ID
    public ReservaResponse obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));
    }

    // LISTAR TODAS LAS RESERVAS QUE PERTENECEN A UN CLIENTE CONCRETO
    public List<ReservaResponse> listarPorCliente(Long clienteId) {
        return reservaRepository.findByClienteId(clienteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // UBICACIÓN CORREGIDA: MÉTODO PARA BUSCAR POR ESTADO TIPADO CON EL ENUM
    public List<ReservaResponse> listarPorEstado(String estado) {
        com.rentamovil.model.enums.EstadoReserva estadoEnum = com.rentamovil.model.enums.EstadoReserva.valueOf(estado.toUpperCase());
        return reservaRepository.findByEstado(estadoEnum).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservaResponse actualizar(Long id, ReservaRequest request) {
        log.info("Actualizando reserva ID: {}", id);

        // EXTRAER RESERVA ACTUAL DESDE LA BASE DE DATOS
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        // RECALCULAR FECHAS Y TOTAL EN CASO DE QUE SE MODIFIQUE EL RANGO TEMPORAL
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

        // EVALUAR EL CAMBIO DE ESTADO DE LA RESERVA Y MANEJAR FLUJOS DE TRABAJO
        if (request.getEstado() != null) {
            // CONVERSIÓN SEGURA DE STRING A ENUM AL MOMENTO DE ACTUALIZAR
            reserva.setEstado(com.rentamovil.model.enums.EstadoReserva.valueOf(request.getEstado().toUpperCase()));

            // AL COMPLETAR O CANCELAR, LIBERAR EL VEHÍCULO AUTOMÁTICAMENTE USANDO ENUMS
            if (com.rentamovil.model.enums.EstadoReserva.COMPLETADA.equals(reserva.getEstado()) ||
                    com.rentamovil.model.enums.EstadoReserva.CANCELADA.equals(reserva.getEstado())) {
                Vehiculo vehiculo = reserva.getVehiculo();
                vehiculo.setEstado(com.rentamovil.model.enums.EstadoVehiculo.DISPONIBLE);
                vehiculoRepository.save(vehiculo);
                log.info("Vehículo {} liberado por reserva {} ({})",
                        vehiculo.getPlaca(), id, request.getEstado());
            }
        }

        // ACTUALIZAR NOTAS SI EL CAMPO NO ES NULO
        if (request.getNotas() != null) {
            reserva.setNotas(request.getNotas());
        }

        return mapToResponse(reservaRepository.save(reserva));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando reserva ID: {}", id);

        // EXTRAER LA RESERVA PARA VERIFICAR SU ESTADO ANTES DE ELIMINAR DE RAIZ
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "id", id));

        // LIBERAR VEHÍCULO SI LA RESERVA ESTABA ACTIVA O PENDIENTE USANDO ENUMS
        if (com.rentamovil.model.enums.EstadoReserva.ACTIVA.equals(reserva.getEstado()) ||
                com.rentamovil.model.enums.EstadoReserva.PENDIENTE.equals(reserva.getEstado())) {
            Vehiculo vehiculo = reserva.getVehiculo();
            vehiculo.setEstado(com.rentamovil.model.enums.EstadoVehiculo.DISPONIBLE);
            vehiculoRepository.save(vehiculo);
        }

        // BORRAR REGISTRO FÍSICO
        reservaRepository.deleteById(id);
    }

    // UBICACIÓN CORREGIDA: CONTADOR OPERATIVO QUE EVALÚA ESTADOS ACTIVOS MAPEADOS CON EL ENUM
    public long countActivas() {
        return reservaRepository.findByEstado(com.rentamovil.model.enums.EstadoReserva.ACTIVA).size() +
                reservaRepository.findByEstado(com.rentamovil.model.enums.EstadoReserva.PENDIENTE).size();
    }

    // ─── MAPPERS INTERNOS CORREGIDOS ──────────────────────────────────────────────────────

    // CONVERTIR LA ENTIDAD RESERVA EN UN DTO RESERVARESPONSE
    private ReservaResponse mapToResponse(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setId(reserva.getId());
        response.setCliente(mapClienteToResponse(reserva.getCliente()));
        response.setVehiculo(mapVehiculoToResponse(reserva.getVehiculo()));
        response.setFechaInicio(reserva.getFechaInicio());
        response.setFechaFin(reserva.getFechaFin());
        response.setTotal(reserva.getTotal());

        // CONVERTIR EL ENUM ESTADORESERVA A STRING USANDO .NAME() EN MINÚSCULAS
        response.setEstado(reserva.getEstado() != null ? reserva.getEstado().name().toLowerCase() : null);

        response.setNotas(reserva.getNotas());
        response.setFechaCreacion(reserva.getFechaCreacion());
        return response;
    }

    // CONVERTIR LA ENTIDAD CLIENTE EN UN DTO CLIENTERESPONSE
    private com.rentamovil.dto.ClienteResponse mapClienteToResponse(Cliente cliente) {
        com.rentamovil.dto.ClienteResponse r = new com.rentamovil.dto.ClienteResponse();
        r.setId(cliente.getId());
        r.setNombres(cliente.getNombres());
        r.setApellidos(cliente.getApellidos());
        r.setTipoDocumento(cliente.getTipoDocumento());

        // CORRECCIÓN CON EL PREFIJO SET PARA EL MÉTODO GENERADO POR LOMBOK
        r.setNumeroDocumento(cliente.getNumeroDocumento());

        r.setTelefono(cliente.getTelefono());
        r.setEmail(cliente.getEmail());
        r.setEstado(cliente.getEstado());
        return r;
    }

    // CONVERTIR LA ENTIDAD VEHÍCULO EN UN DTO VEHICULORESPONSE
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

        // CONVERTIR EL ENUM ESTADOVEHICULO A STRING USANDO .NAME() EN MINÚSCULAS
        r.setEstado(vehiculo.getEstado() != null ? vehiculo.getEstado().name().toLowerCase() : null);

        return r;
    }
}
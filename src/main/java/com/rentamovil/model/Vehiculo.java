package com.rentamovil.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculos")
@Data
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, unique = true)
    private String placa;

    private String color;

    @Column(name = "kilometraje")
    private Integer kilometraje = 0;

    @Column(name = "precio_dia", nullable = false)
    private BigDecimal precioDia;

    @Column(nullable = false)
    private String estado = "disponible";

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
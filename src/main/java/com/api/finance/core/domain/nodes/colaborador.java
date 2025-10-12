package com.api.finance.core.domain.nodes;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.UUID;

public class colaborador {

    @Serial
    private static final long serialVersionUID = 1L;

    // 🆔 ID principal
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long empresacliente;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean isAdministrator;

    @Column(nullable = false)
    private boolean isGerente;


    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;
}

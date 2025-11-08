package com.api.finance.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "tb_acrescimo_atraso")
public class AcrescimoPorAtraso {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long emprestimoId; // referência ao empréstimo

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal valorAtraso; // valor do atraso


    @Column
    private String dataRegistro; // "yyyy-MM-dd", opcional para registrar quando foi gerado

    // Construtor padrão
    public AcrescimoPorAtraso() {
    }
}

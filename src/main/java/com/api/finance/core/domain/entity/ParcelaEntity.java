package com.api.finance.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "tb_parcela")
public class ParcelaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long emprestimoId; // referência via código para o empréstimo

    @Column
    private Integer numeroParcela; // número da parcela

    @Column
    private String vencimento; // "yyyy-MM-dd"

    @Column
    private int status; // 0 = em aberto, 1 = paga

    @Column(precision = 15, scale = 2)
    private BigDecimal valorOriginal; // valor da parcela

    @Column(precision = 15, scale = 2)
    private BigDecimal valorPago; // valor pago, se tiver

    @Column(precision = 15, scale = 2)
    private BigDecimal valorDesconto; // desconto aplicado

    @Column(precision = 5, scale = 2)
    private BigDecimal juros; // juros se vencida

    @Column
    private UUID usuarioUuid; // UUID do usuário que gerou o pagamento

    @Column
    private String dataPagamento; // "yyyy-MM-dd" quando paga, null se em aberto

    @Column
    private Boolean parcelaAdicional = false; // indica se é parcela adicional

    // Construtor padrão
    public ParcelaEntity() {
    }
}
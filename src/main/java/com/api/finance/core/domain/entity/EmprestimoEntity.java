package com.api.finance.core.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "tb_emprestimo")
@Where(clause = "deletado <> true")
public class EmprestimoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String cliente;

    @Column
    private String contato;
    @Column
    private String contatoCobranca;

    @Column(precision = 15, scale = 2)
    private BigDecimal valor;

    @Column
    private int tipoEmprestimo; // 1=Juros Simples, 2=Juros Composto, 3=Especial

    @Column(precision = 5, scale = 2)
    private BigDecimal taxaJuros;

    @Column(precision = 5, scale = 2)
    private BigDecimal taxaJurosParcelaAtraso;

    @Column
    private Integer quantidadeParcelas;

    @Column
    private String vencimentoPrimeiraParcela; // "yyyy-MM-dd"

    @Column
    private int tipoCobranca; // 1=Semanal, 2=Quinzenal, 3=Mensal

    @Column
    private UUID usuarioId; // referência via código

    @Column
    private Long empresaId; // referência via código

    @Column
    private String dataFechamento; // "yyyy-MM-dd"

    @Column
    private Boolean deletado = false; // nova coluna para soft delete

    // Construtor padrão para JPA
    public EmprestimoEntity() {
    }

}

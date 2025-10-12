package com.api.finance.core.dto;

import com.api.finance.core.utils.enums.TipoCobranca;
import com.api.finance.core.utils.enums.TipoEmprestimo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class EmprestimoDTO {

    private Long id;
    private String cliente;
    private String contato;
    private BigDecimal valor;
    private Integer tipoEmprestimo;
    private BigDecimal taxaJuros;
    private Integer quantidadeParcelas;
    private LocalDate vencimentoPrimeiraParcela;
    private Integer tipoCobranca;
    private Long usuarioId;
    private LocalDate dataFechamento;

}

package com.api.finance.core.dto;

import com.api.finance.core.utils.enums.TipoCobranca;
import com.api.finance.core.utils.enums.TipoEmprestimo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmprestimoDTO {

    private Long id;
    private String cliente;
    private String contato;
    private String contatoCobranca;
    private BigDecimal valor;
    private Integer tipoEmprestimo;
    private BigDecimal taxaJuros;
    private BigDecimal taxaJurosAtrasoParcela;
    private Integer quantidadeParcelas;
    private LocalDate vencimentoPrimeiraParcela;
    private Integer tipoCobranca;
    private UUID usuarioId;
    private LocalDate dataFechamento;
    private BigDecimal valorResidual;
    private List<ParcelaDTO> parcelas;

}

package com.api.finance.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ParcelaDTO {

    private Long id;
    private Long emprestimoId;
    private Integer numeroParcela;
    private String vencimento;          // formato "yyyy-MM-dd"
    private int status;                 // 1 = em aberto, 2 = paga
    private BigDecimal valorOriginal;   // valor base da parcela
    private BigDecimal valorPago;       // valor efetivamente pago
    private BigDecimal valorDesconto;   // desconto aplicado
    private BigDecimal juros;           // juros (se aplicável)
    private UUID usuarioUuid;           // usuário que efetuou o pagamento
    private String dataPagamento;       // data do pagamento ("yyyy-MM-dd")
    public ParcelaDTO() {
    }
}

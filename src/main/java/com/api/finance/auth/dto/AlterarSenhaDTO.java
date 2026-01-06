package com.api.finance.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class AlterarSenhaDTO {
    private String senhaAtual;
    private String novaSenha;

}

package com.api.finance.core.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoEmprestimo {
    JUROS_SIMPLES(1, "Juros Simples"),
    JUROS_COMPOSTO(2, "Juros Composto"),
    ESPECIAL(3, "Especial");

    private final int code;
    private final String descricao;

    TipoEmprestimo(int code, String descricao) {
        this.code = code;
        this.descricao = descricao;
    }

    public int getCode() {
        return code;
    }

    public String getDescricao() {
        return descricao;
    }

    // Converte código para enum
    public static TipoEmprestimo fromCode(int code) {
        for (TipoEmprestimo tipo : TipoEmprestimo.values()) {
            if (tipo.code == code) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de TipoEmprestimo inválido: " + code);
    }

    // Para JSON
    @JsonValue
    public String toJson() {
        return this.descricao;
    }

    @JsonCreator
    public static TipoEmprestimo fromJson(String descricao) {
        for (TipoEmprestimo tipo : TipoEmprestimo.values()) {
            if (tipo.descricao.equalsIgnoreCase(descricao)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Descrição de TipoEmprestimo inválida: " + descricao);
    }
}

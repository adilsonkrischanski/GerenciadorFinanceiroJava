package com.api.finance.core.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoCobranca {
    SEMANAL(1, "Semanal"),
    QUINZENAL(2, "Quinzenal"),
    MENSAL(3, "Mensal");

    private final int code;
    private final String descricao;

    TipoCobranca(int code, String descricao) {
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
    public static TipoCobranca fromCode(int code) {
        for (TipoCobranca tipo : TipoCobranca.values()) {
            if (tipo.code == code) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de TipoCobranca inválido: " + code);
    }

    // Para salvar no banco via JPA usando @Enumerated ou converter manualmente
    @JsonValue
    public String toJson() {
        return this.descricao;
    }

    @JsonCreator
    public static TipoCobranca fromJson(String descricao) {
        for (TipoCobranca tipo : TipoCobranca.values()) {
            if (tipo.descricao.equalsIgnoreCase(descricao)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Descrição de TipoCobranca inválida: " + descricao);
    }
}

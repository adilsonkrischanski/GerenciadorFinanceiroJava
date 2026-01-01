package com.api.finance.core.utils.enums;

public enum StatusParcela {

    PENDENTE(1, "Pendente"),
    PAGA(2, "Paga"),
    LIQUIDADO(3, "Liquidado"),
    REALOCADO(4, "Realocada");

    private final int code;
    private final String descricao;

    StatusParcela(int code, String descricao) {
        this.code = code;
        this.descricao = descricao;
    }

    public int getCode() {
        return code;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusParcela fromCode(int code) {
        for (StatusParcela status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Código de status inválido: " + code);
    }

    @Override
    public String toString() {
        return descricao;
    }
}

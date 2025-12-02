package com.api.finance.core.services.sistema;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculadoraJuros {

    public static BigDecimal calcularParcela(BigDecimal valor, int quantidadeParcelas, BigDecimal taxaJuros, int tipoJuros) {
        switch (tipoJuros) {
            case 1:
                return calcularJurosSimples(valor, quantidadeParcelas, taxaJuros);
            case 2:
                return calcularJurosComposto(valor, quantidadeParcelas, taxaJuros);
            case 3:
                return calcularJurosEspecial(valor, taxaJuros);
            default:
                throw new IllegalArgumentException("Tipo de juros inválido: " + tipoJuros);
        }
    }

    /**
     * Calcula o valor de uma parcela com juros simples.
     */
    private static BigDecimal calcularJurosSimples(BigDecimal valor, int quantidadeParcelas, BigDecimal taxaJuros) {
        BigDecimal jurosTotal = valor.multiply(taxaJuros)
                .multiply(BigDecimal.valueOf(quantidadeParcelas))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal valorTotal = valor.add(jurosTotal);
        return valorTotal.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor de uma parcela com juros compostos.
     */
    private static BigDecimal calcularJurosComposto(BigDecimal valor, int quantidadeParcelas, BigDecimal taxaJuros) {
        // Converte a taxa percentual (ex: 10) em decimal (0.10)
        BigDecimal taxaDecimal = taxaJuros.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // Calcula o fator (1 + i)^n
        BigDecimal fator = BigDecimal.ONE.add(taxaDecimal).pow(quantidadeParcelas);

        // Fórmula da Tabela Price:
        // Parcela = valor * [i * (1 + i)^n] / [(1 + i)^n - 1]
        BigDecimal numerador = valor.multiply(taxaDecimal).multiply(fator);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);

        // Retorna a parcela com 2 casas decimais
        return numerador.divide(new BigDecimal(quantidadeParcelas/10), 2, RoundingMode.HALF_UP);
    }


    /**
     * Calcula o valor de uma parcela para juros especial (a taxa é a própria parcela).
     */
    private static BigDecimal calcularJurosEspecial(BigDecimal valor, BigDecimal taxaJuros) {
        BigDecimal value = valor.multiply(taxaJuros.divide(new BigDecimal(100)));
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

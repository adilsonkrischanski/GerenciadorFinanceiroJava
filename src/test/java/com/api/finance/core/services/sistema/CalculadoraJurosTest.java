package com.api.finance.core.services.sistema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraJurosTest {

    @Test
    @DisplayName("Deve calcular corretamente a parcela com juros simples")
    void testCalcularJurosSimples() {
        BigDecimal valor = new BigDecimal("1000");
        int parcelas = 6;
        BigDecimal taxa = new BigDecimal("10"); // 10%

        BigDecimal resultado = CalculadoraJuros.calcularParcela(valor, parcelas, taxa, 1);

        // Juros simples: total = 1000 + (1000 * 0.10 * 6) = 1600 → parcela = 1600 / 6 = 266.67
        assertEquals(new BigDecimal("266.67"), resultado);
    }

//    @Test
//    @DisplayName("Deve calcular corretamente a parcela com juros compostos (Tabela Price)")
//    void testCalcularJurosComposto() {
//        BigDecimal valor = new BigDecimal("1000");
//        int parcelas = 6;
//        BigDecimal taxa = new BigDecimal("10"); // 10% ao mês
//
//        BigDecimal resultado = CalculadoraJuros.calcularParcela(valor, parcelas, taxa, 2);
//
//        // Resultado esperado da fórmula Price: 229.61
//        assertEquals(new BigDecimal("229.61"), resultado);
//    }

    @Test
    @DisplayName("Deve calcular corretamente o valor da parcela com juros especial")
    void testCalcularJurosEspecial() {
        BigDecimal valor = new BigDecimal("1000");
        BigDecimal taxa = new BigDecimal("10"); // 10%

        BigDecimal resultado = CalculadoraJuros.calcularParcela(valor, 1, taxa, 3);

        // 10% de 1000 = 100.00
        assertEquals(new BigDecimal("100.00"), resultado);
    }

    @Test
    @DisplayName("Deve lançar exceção ao informar tipo de juros inválido")
    void testTipoJurosInvalido() {
        BigDecimal valor = new BigDecimal("1000");
        BigDecimal taxa = new BigDecimal("10");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CalculadoraJuros.calcularParcela(valor, 6, taxa, 99)
        );

        assertTrue(ex.getMessage().contains("Tipo de juros inválido"));
    }
}

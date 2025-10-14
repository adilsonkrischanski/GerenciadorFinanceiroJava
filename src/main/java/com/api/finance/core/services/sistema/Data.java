package com.api.finance.core.services.sistema;

import com.api.finance.core.utils.enums.TipoCobranca;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Data {

    private LocalDate data;
    private static final DateTimeFormatter FORMATO_PADRAO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Construtor padrão (data atual)
    public Data() {
        this.data = LocalDate.now();
    }

    // Construtor a partir de String (formato padrão)
    public Data(String dataBanco) {
        this(dataBanco, FORMATO_PADRAO);
    }

    // Construtor com formato personalizado
    public Data(String dataBanco, DateTimeFormatter formato) {
        try {
            this.data = LocalDate.parse(dataBanco, formato);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data inválido. Use yyyy-MM-dd ou especifique outro formato.");
        }
    }

    // Somar dias
    public void somarDias(int dias) {
        this.data = this.data.plusDays(dias);
    }

    // Somar 1, 7 e 14 dias (atalhos)
    public void somarUmDia() { somarDias(1); }
    public void somarSeteDias() { somarDias(7); }
    public void somarQuatorzeDias() { somarDias(14); }

    // Somar meses
    public void somarMeses(int meses) {
        this.data = this.data.plusMonths(meses);
    }

    public int getDia() { return data.getDayOfMonth(); }
    public int getMes() { return data.getMonthValue(); }
    public int getAno() { return data.getYear(); }

    @Override
    public String toString() {
        return data.format(FORMATO_PADRAO);
    }

    public LocalDate toLocalDate() {
        return data;
    }

    public static String gerarDataVencimento(Data data, int numeroParcela, TipoCobranca tipoCobranca) {
        // Criamos uma cópia da data inicial para não modificar o objeto original
        Data novaData = new Data(data.toString());

        switch (tipoCobranca) {
            case SEMANAL:
                novaData.somarDias(7 * (numeroParcela - 1));
                break;

            case QUINZENAL:
                novaData.somarDias(14 * (numeroParcela - 1));
                break;

            case MENSAL:
                novaData.somarMeses(numeroParcela - 1);
                break;

            default:
                throw new IllegalArgumentException("Tipo de cobrança inválido: " + tipoCobranca);
        }

        return novaData.toString();
    }
}

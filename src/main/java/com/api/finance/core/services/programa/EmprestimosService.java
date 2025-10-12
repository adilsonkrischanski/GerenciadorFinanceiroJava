package com.api.finance.core.services.programa;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.EmprestimoDTO;
import com.api.finance.core.services.tabela.ParcelaService;
import com.api.finance.core.utils.enums.StatusParcela;
import com.api.finance.core.utils.enums.TipoCobranca;
import com.api.finance.core.utils.enums.TipoEmprestimo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.api.finance.core.services.sistema.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class EmprestimosService {

    @Autowired
    com.api.finance.core.services.tabela.EmprestimoService emprestimoTabelaService;

    @Autowired
    ParcelaService parcelaService;


    public ResponseEntity<Map<String, String>> criarEmprestimo(UserEntity userGestor, EmprestimoDTO body) {

        EmprestimoEntity emprestimo = new EmprestimoEntity();

        emprestimo.setCliente(body.getCliente());
        emprestimo.setContato(body.getContato());
        emprestimo.setValor(body.getValor());
        emprestimo.setUsuarioId(UUID.randomUUID());
        emprestimo.setTaxaJuros(body.getTaxaJuros());
        emprestimo.setTipoEmprestimo(TipoEmprestimo.fromCode(body.getTipoEmprestimo()).getCode());
        emprestimo.setEmpresaId(userGestor.getEmpresacliente());
        emprestimo.setTipoCobranca(TipoCobranca.fromCode(body.getTipoCobranca()).getCode());
        emprestimo.setDataFechamento(new Date().toString());
        emprestimo.setQuantidadeParcelas(body.getQuantidadeParcelas());
        emprestimo.setVencimentoPrimeiraParcela(body.getVencimentoPrimeiraParcela().toString());
        emprestimoTabelaService.save(emprestimo);
        gerarParcelas(userGestor, emprestimo);

        Map<String, String> response = new HashMap<>();
        response.put("result", "Emprestimo cadastrado");
        return ResponseEntity.ok(response);
    }

    private void gerarParcelas(UserEntity userGestor, EmprestimoEntity emprestimo) {
        BigDecimal valorParcelas = calcularParcela(emprestimo.getValor(),emprestimo.getQuantidadeParcelas(),emprestimo.getTaxaJuros(),emprestimo.getTipoEmprestimo());
        for(int i=1; i< emprestimo.getQuantidadeParcelas();i++){
            ParcelaEntity parcela = new ParcelaEntity();
            parcela.setEmprestimoId(emprestimo.getId());
            parcela.setNumeroParcela(i);
            String vencimento = gerarDataVencimento(new Data(emprestimo.getVencimentoPrimeiraParcela()),i, TipoCobranca.fromCode(emprestimo.getTipoCobranca()));
            parcela.setVencimento(vencimento.toString());
            parcela.setStatus(StatusParcela.PENDENTE.getCode());
            parcela.setValorOriginal(valorParcelas);
            parcela.setValorPago(BigDecimal.ZERO);
            parcela.setValorDesconto(BigDecimal.ZERO);
            parcela.setJuros(BigDecimal.ZERO); //jurosPorVencimento
            parcela.setUsuarioUuid(userGestor.getId());
            parcela.setDataPagamento(null);
            parcelaService.save(parcela);

        }

    }

    private String gerarDataVencimento(Data data, int numeroParcela, TipoCobranca tipoCobranca) {
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



    public BigDecimal calcularParcela(BigDecimal valor, int quantidadeParcelas, BigDecimal taxaJuros, int tipoJuros) {
        switch (tipoJuros) {
            case 1:
                return calcularJurosSimples(valor, quantidadeParcelas, taxaJuros);
            case 2:
                return calcularJurosComposto(valor, quantidadeParcelas, taxaJuros);
            case 3:
                return calcularJurosEspecial(taxaJuros);
            default:
                throw new IllegalArgumentException("Tipo de juros inválido: " + tipoJuros);
        }
    }

    /**
     * Calcula o valor de uma parcela com juros simples.
     */
    private BigDecimal calcularJurosSimples(BigDecimal valor, int quantidadeParcelas, BigDecimal taxaJuros) {
        BigDecimal jurosTotal = valor.multiply(taxaJuros)
                .multiply(BigDecimal.valueOf(quantidadeParcelas))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal valorTotal = valor.add(jurosTotal);
        return valorTotal.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor de uma parcela com juros compostos.
     */
    private BigDecimal calcularJurosComposto(BigDecimal valor, int quantidadeParcelas, BigDecimal taxaJuros) {
        BigDecimal taxaDecimal = taxaJuros.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal fator = BigDecimal.ONE.add(taxaDecimal).pow(quantidadeParcelas);
        BigDecimal parcela = valor.multiply(taxaDecimal).multiply(fator)
                .divide(fator.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        return parcela;
    }

    /**
     * Calcula o valor de uma parcela para juros especial (a taxa é a própria parcela).
     */
    private BigDecimal calcularJurosEspecial(BigDecimal taxaJuros) {
        return taxaJuros.setScale(2, RoundingMode.HALF_UP);
    }

    public List<EmprestimoEntity> listarEmprestimosCorrentes(UserEntity userGestor) {
        // Busca todos os empréstimos da empresa do gestor logado
        List<EmprestimoEntity> todosEmprestimos = emprestimoTabelaService.findByEmpresaId(userGestor.getEmpresacliente());

        // Filtra apenas os que têm parcelas pendentes
        return todosEmprestimos.stream()
                .filter(e -> parcelaService.temParcelasPendentes(e.getId()))
                .toList();
    }

}

package com.api.finance.core.services.programa;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.EmprestimoDTO;
import com.api.finance.core.services.sistema.CalculadoraJuros;
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
        BigDecimal valorParcelas = CalculadoraJuros.calcularParcela(emprestimo.getValor(), emprestimo.getQuantidadeParcelas(), emprestimo.getTaxaJuros(), emprestimo.getTipoEmprestimo());
        if (emprestimo.getTipoEmprestimo() == TipoEmprestimo.ESPECIAL.getCode()) {
            emprestimo.setQuantidadeParcelas(1);
        }
        for (int i = 0; i < emprestimo.getQuantidadeParcelas(); i++) {
            ParcelaEntity parcela = new ParcelaEntity();
            parcela.setEmprestimoId(emprestimo.getId());
            parcela.setNumeroParcela(i + 1);
            String vencimento = Data.gerarDataVencimento(new Data(emprestimo.getVencimentoPrimeiraParcela()), i, TipoCobranca.fromCode(emprestimo.getTipoCobranca()));
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




    public List<EmprestimoEntity> listarEmprestimosCorrentes(UserEntity userGestor) {
        // Busca todos os empréstimos da empresa do gestor logado
        List<EmprestimoEntity> todosEmprestimos = emprestimoTabelaService.findByEmpresaId(userGestor.getEmpresacliente());

        // Filtra apenas os que têm parcelas pendentes
        return todosEmprestimos.stream()
                .filter(e -> parcelaService.temParcelasPendentes(e.getId()))
                .toList();
    }

}

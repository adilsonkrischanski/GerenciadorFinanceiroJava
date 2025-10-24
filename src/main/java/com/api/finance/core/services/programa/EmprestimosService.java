package com.api.finance.core.services.programa;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.EmprestimoDTO;
import com.api.finance.core.services.sistema.CalculadoraJuros;
import com.api.finance.core.services.sistema.Data;
import com.api.finance.core.services.tabela.ParcelaService;
import com.api.finance.core.utils.enums.StatusParcela;
import com.api.finance.core.utils.enums.TipoCobranca;
import com.api.finance.core.utils.enums.TipoEmprestimo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmprestimosService {

    @Autowired
    com.api.finance.core.services.tabela.EmprestimoService emprestimoTabelaService;

    @Autowired
    ParcelaService parcelaService;

    @Autowired
    ParcelasService parcelasService;

    // -----------------------------------------------------------
    // CRIAÇÃO DE EMPRÉSTIMO
    // -----------------------------------------------------------
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
        emprestimo.setDataFechamento(new Data().toString());
        emprestimo.setQuantidadeParcelas(body.getQuantidadeParcelas());
        emprestimo.setVencimentoPrimeiraParcela(body.getVencimentoPrimeiraParcela().toString());

        emprestimoTabelaService.save(emprestimo);
        gerarParcelas(userGestor, emprestimo);

        Map<String, String> response = new HashMap<>();
        response.put("result", "Empréstimo cadastrado com sucesso");
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------
    // GERAÇÃO DAS PARCELAS
    // -----------------------------------------------------------
    private void gerarParcelas(UserEntity userGestor, EmprestimoEntity emprestimo) {
        BigDecimal valorParcelas = CalculadoraJuros.calcularParcela(
                emprestimo.getValor(),
                emprestimo.getQuantidadeParcelas(),
                emprestimo.getTaxaJuros(),
                emprestimo.getTipoEmprestimo()
        );

        if (emprestimo.getTipoEmprestimo() == TipoEmprestimo.ESPECIAL.getCode()) {
            emprestimo.setQuantidadeParcelas(1);
        }

        for (int i = 1; i <= emprestimo.getQuantidadeParcelas(); i++) {
            ParcelaEntity parcela = new ParcelaEntity();
            parcela.setEmprestimoId(emprestimo.getId());
            parcela.setNumeroParcela(i);

            // Usando a classe Data para gerar o vencimento
            Data dataBase = new Data(emprestimo.getVencimentoPrimeiraParcela());
            String vencimento = Data.gerarDataVencimento(dataBase, i, TipoCobranca.fromCode(emprestimo.getTipoCobranca()));
            parcela.setVencimento(vencimento);

            parcela.setStatus(StatusParcela.PENDENTE.getCode());
            parcela.setValorOriginal(valorParcelas);
            parcela.setValorPago(BigDecimal.ZERO);
            parcela.setValorDesconto(BigDecimal.ZERO);
            parcela.setJuros(BigDecimal.ZERO);
            parcela.setUsuarioUuid(userGestor.getId());
            parcela.setDataPagamento(null);

            parcelaService.save(parcela);
        }
    }

    // -----------------------------------------------------------
    // LISTAGEM DE EMPRÉSTIMOS (Entity → DTO)
    // -----------------------------------------------------------
    public List<EmprestimoDTO> listarEmprestimosCorrentesDTO(UserEntity userGestor) {
        List<EmprestimoEntity> emprestimos = listarEmprestimosCorrentes(userGestor);

        return emprestimos.stream()
                .sorted(Comparator.comparing(EmprestimoEntity::getId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private EmprestimoDTO toDTO(EmprestimoEntity e) {
        Data vencimento = null;
        Data fechamento = null;

        try {
            if (e.getVencimentoPrimeiraParcela() != null) {
                vencimento = new Data(e.getVencimentoPrimeiraParcela());
            }
        } catch (Exception ignored) {}

        try {
            if (e.getDataFechamento() != null) {
                fechamento = new Data(e.getDataFechamento());
            }
        } catch (Exception ignored) {}

        return new EmprestimoDTO(
                e.getId(),
                e.getCliente(),
                e.getContato(),
                e.getValor(),
                e.getTipoEmprestimo(),
                e.getTaxaJuros(),
                e.getQuantidadeParcelas(),
                vencimento != null ? vencimento.toLocalDate() : null,
                e.getTipoCobranca(),
                null, // ou converter UUID se quiser mostrar
                fechamento != null ? fechamento.toLocalDate() : null,
                parcelasService.calculaValorResidual(e)
        );
    }

    // -----------------------------------------------------------
    // FILTRO DE EMPRÉSTIMOS ATIVOS
    // -----------------------------------------------------------
    public List<EmprestimoEntity> listarEmprestimosCorrentes(UserEntity userGestor) {
        List<EmprestimoEntity> todosEmprestimos =
                emprestimoTabelaService.findByEmpresaId(userGestor.getEmpresacliente());

        return todosEmprestimos.stream()
                .filter(e -> parcelaService.temParcelasPendentes(e.getId()))
                .collect(Collectors.toList());
    }

}

package com.api.finance.core.services.programa;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.ParcelaDTO;
import com.api.finance.core.services.sistema.CalculadoraJuros;
import com.api.finance.core.services.sistema.Data;
import com.api.finance.core.services.tabela.EmprestimoService;
import com.api.finance.core.utils.enums.StatusParcela;
import com.api.finance.core.utils.enums.TipoCobranca;
import com.api.finance.core.utils.enums.TipoEmprestimo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ParcelasService {

    @Autowired
    com.api.finance.core.services.tabela.ParcelaService parcelaService;
    @Autowired
    EmprestimoService emprestimosService;


    public List<ParcelaEntity> buscarPorEmprestimoId(Long emprestimoId) {
        return parcelaService.findByEmprestimoId(emprestimoId);
    }

    public ParcelaDTO buscarDtoPorId(Long id) {
        ParcelaDTO parcelaDTO = gerarDtoPorEntity(parcelaService.findById(id));
        return parcelaDTO;

    }

    private ParcelaDTO gerarDtoPorEntity(ParcelaEntity parcela) {
        ParcelaDTO parcelaDTO = new ParcelaDTO();
        parcelaDTO.setId(parcela.getId());
        parcelaDTO.setEmprestimoId(parcela.getEmprestimoId());
        parcelaDTO.setNumeroParcela(parcela.getNumeroParcela());
        parcelaDTO.setVencimento(parcela.getVencimento());
        parcelaDTO.setStatus(parcela.getStatus());
        parcelaDTO.setValorOriginal(parcela.getValorOriginal());
        parcelaDTO.setValorPago(parcela.getValorPago());
        parcelaDTO.setValorDesconto(parcela.getValorDesconto());
        parcelaDTO.setJuros(parcela.getJuros());
        parcelaDTO.setUsuarioUuid(parcela.getUsuarioUuid());
        parcelaDTO.setDataPagamento(parcela.getDataPagamento());

        return parcelaDTO;


    }

    public boolean confirmarPagamento(UserEntity user, ParcelaDTO dto) throws Exception {
        // Busca a parcela no banco
        ParcelaEntity parcela = parcelaService.findById(dto.getId());

        // Validação: parcela inexistente ou já paga
        if (parcela == null || parcela.getStatus() == StatusParcela.PAGA.getCode()) {
            return false;
        }

        // Atualiza informações básicas
        parcela.setDataPagamento(new Data().toString());
        parcela.setUsuarioUuid(user.getId());

        // Define valores monetários
        BigDecimal desconto = dto.getValorDesconto() != null ? dto.getValorDesconto() : BigDecimal.ZERO;
        BigDecimal valorPago = dto.getValorPago() != null ? dto.getValorPago() : BigDecimal.ZERO;

        parcela.setValorDesconto(desconto);
        parcela.setValorPago(valorPago);

        // Verifica se o total pago + desconto cobre o valor da parcela original
        BigDecimal valorTotalPago = valorPago.add(desconto);
        if (valorTotalPago.compareTo(parcela.getValorDesconto()) >= 0) {
            parcela.setStatus(StatusParcela.PAGA.getCode());
        } else {
            parcela.setStatus(StatusParcela.PENDENTE.getCode());
        }

        // Persiste a atualização no banco
        parcelaService.save(parcela);

        EmprestimoEntity emprestimo = buscarEmprestimo(parcela);
        if (emprestimo != null) {
            gerarNovaParcela(user, emprestimo);
        }


        return true;
    }


    private void gerarNovaParcela(UserEntity user, EmprestimoEntity emprestimo) {
        List<ParcelaEntity> parcelaEntities = parcelaService.buscarPorEmprestimoId(emprestimo.getId());
        BigDecimal saldoDevedor = saldoDevedor(emprestimo, parcelaEntities);
        if (saldoDevedor.compareTo(BigDecimal.ZERO) <= 0) {
            emprestimo.setDataFechamento(new Data().toString());
            emprestimosService.save(emprestimo);
            return;
        }
        BigDecimal parcelaAtual = CalculadoraJuros.calcularParcela(saldoDevedor, 1, emprestimo.getTaxaJuros(), emprestimo.getTipoCobranca());
        novaParcelaEspecial(user, emprestimo, parcelaAtual, parcelaEntities.size() + 1);


    }

    private BigDecimal saldoDevedor(EmprestimoEntity emprestimo, List<ParcelaEntity> parcelaEntities) {
        BigDecimal acumulalator = BigDecimal.ZERO;
        for (ParcelaEntity parcela : parcelaEntities) {
            BigDecimal total = parcela.getValorPago().add(parcela.getValorDesconto());
            BigDecimal sobra = total.subtract(parcela.getValorOriginal());
            acumulalator = acumulalator.add(sobra);
        }
        return emprestimo.getValor().subtract(acumulalator);
    }

    private void novaParcelaEspecial(UserEntity user, EmprestimoEntity emprestimo, BigDecimal valor, int parcelaNumero) {
        ParcelaEntity parcela = new ParcelaEntity();
        parcela.setEmprestimoId(emprestimo.getId());
        parcela.setNumeroParcela(parcelaNumero);
        String vencimento = Data.gerarDataVencimento(new Data(emprestimo.getVencimentoPrimeiraParcela()), parcelaNumero, TipoCobranca.fromCode(emprestimo.getTipoCobranca()));
        parcela.setVencimento(vencimento.toString());
        parcela.setStatus(StatusParcela.PENDENTE.getCode());
        parcela.setValorOriginal(valor);
        parcela.setValorPago(BigDecimal.ZERO);
        parcela.setValorDesconto(BigDecimal.ZERO);
        parcela.setJuros(BigDecimal.ZERO); //jurosPorVencimento
        parcela.setUsuarioUuid(user.getId());
        parcela.setDataPagamento(null);
        parcelaService.save(parcela);
    }


    private EmprestimoEntity buscarEmprestimo(ParcelaEntity parcela) throws Exception {

        Optional<EmprestimoEntity> emprestimoEntityOptional = emprestimosService.findById(parcela.getEmprestimoId());

        if (emprestimoEntityOptional.isEmpty()) {
            throw new Exception("parcela nao atrelada a um emprestimo");
        }

        EmprestimoEntity emprestimo = emprestimoEntityOptional.get();
        if (emprestimo.getTipoEmprestimo() == TipoEmprestimo.ESPECIAL.getCode())
            return emprestimo;
        return null;
    }


}

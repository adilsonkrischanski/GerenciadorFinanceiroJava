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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        if (parcela.getValorOriginal().compareTo(valorTotalPago) <= 0) {
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

    public BigDecimal calculaValorResidual(EmprestimoEntity emprestimo) {

        // Busca todas as parcelas vinculadas ao empréstimo
        List<ParcelaEntity> parcelas = parcelaService.findByEmprestimoId(emprestimo.getId());
        if (emprestimo.getTipoEmprestimo() == TipoEmprestimo.ESPECIAL.getCode()) {
            return  saldoDevedor(emprestimo,parcelas);
        }
        else {
            BigDecimal falta = BigDecimal.ZERO;
            for (ParcelaEntity parcela : parcelas) {
                if (parcela.getStatus() == StatusParcela.PAGA.getCode()) {
                    continue;
                }
                falta = falta.add(parcela.getValorOriginal());
            }
            return   falta;
        }
    }


    // ------------------- Total pago no dia -------------------
    public BigDecimal getTotalPagoNoDia(LocalDate hoje) {
        String dataStr = hoje.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return parcelaService.findByStatus(StatusParcela.PAGA.getCode())
                .stream()
                .filter(p -> p.getDataPagamento() != null && p.getDataPagamento().equals(dataStr))
                .map(p -> p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------- Previsão da semana -------------------
    public BigDecimal getPrevisaoSemana(LocalDate hoje) {
        LocalDate semanaFinal = hoje.plusDays(7);
        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> {
                    LocalDate vencimento = LocalDate.parse(p.getVencimento());
                    return !vencimento.isBefore(hoje) && !vencimento.isAfter(semanaFinal);
                })
                .map(p -> p.getValorOriginal() != null ? p.getValorOriginal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------- Quantidade de contratos ativos -------------------
    public long getContratosAtivosCount(Long idEmpresa) {
        // Considera ativo se tem parcelas pendentes
        List<EmprestimoEntity> emprestimos = emprestimosService.findByEmpresaId(idEmpresa);
        return emprestimos.stream()
                .filter(e -> parcelaService.temParcelasPendentes(e.getId()))
                .count();
    }

    // ------------------- Contratos com parcelas vencidas -------------------
    public long getParcelasVencidasCount(LocalDate hoje) {
        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> LocalDate.parse(p.getVencimento()).isBefore(hoje))
                .map(ParcelaEntity::getEmprestimoId)
                .distinct()
                .count();
    }

    // ------------------- Faturamento previsto para 7 dias -------------------
    public BigDecimal getFaturamento7Dias(LocalDate hoje) {
        LocalDate semanaFinal = hoje.plusDays(7);
        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> {
                    LocalDate vencimento = LocalDate.parse(p.getVencimento());
                    return !vencimento.isBefore(hoje) && !vencimento.isAfter(semanaFinal);
                })
                .map(p -> p.getValorOriginal() != null ? p.getValorOriginal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPrevisaoRecebimento(LocalDate hoje, int dias) {
        LocalDate limite = hoje.plusDays(dias);
        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> {
                    LocalDate vencimento = LocalDate.parse(p.getVencimento());
                    return !vencimento.isBefore(hoje) && !vencimento.isAfter(limite);
                })
                .map(p -> p.getValorOriginal() != null ? p.getValorOriginal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}


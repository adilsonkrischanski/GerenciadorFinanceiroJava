package com.api.finance.core.services.programa;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.AcrescimoPorAtraso;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.ParcelaDTO;
import com.api.finance.core.repositories.AcrescimoPorAtrasoRepository;
import com.api.finance.core.services.sistema.CalculadoraJuros;
import com.api.finance.core.services.sistema.Data;
import com.api.finance.core.services.tabela.EmprestimoService;
import com.api.finance.core.services.tabela.ParcelaService;
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
import java.util.stream.Collectors;

@Service
public class ParcelasService {

    @Autowired
    com.api.finance.core.services.tabela.ParcelaService parcelaService;
    @Autowired
    EmprestimoService emprestimosService;

    @Autowired
    AcrescimoPorAtrasoRepository acrescimoPorAtrasoRepository;


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
        // Busca a parcela
        ParcelaEntity parcela = parcelaService.findById(dto.getId());
        if (parcela == null) {
            throw new IllegalArgumentException("Parcela não encontrada.");
        }

        // Verifica se já foi paga
        if (parcela.getStatus() == StatusParcela.PAGA.getCode()) {
            return false;
        }

        // Define data e usuário
        parcela.setDataPagamento(new Data().toString());
        parcela.setUsuarioUuid(user.getId());

        // Atualiza os valores da parcela
        parcela.setValorPago(dto.getValorPago());
        parcela.setValorDesconto(dto.getValorDesconto());

        // Calcula o total pago para comparar com o valor original
        BigDecimal totalPago = parcela.getValorPago().add(parcela.getValorDesconto());// valor pago nesta transação

        // Verifica se o valor pago cobre a parcela
        if (parcela.getValorOriginal().compareTo(totalPago) <= 0) {
            parcela.setStatus(StatusParcela.PAGA.getCode());

        } else {
            Optional<EmprestimoEntity> emprestimoEntityOptional = emprestimosService.findById(parcela.getEmprestimoId());
            EmprestimoEntity emprestimo = emprestimoEntityOptional.get();
            parcela.setStatus(StatusParcela.REALOCADO.getCode());

            if (emprestimo.getTipoEmprestimo() == (TipoEmprestimo.ESPECIAL.getCode())) {
                parcela.setStatus(StatusParcela.REALOCADO.getCode());
                AcrescimoPorAtraso acrescimoPorAtraso = new AcrescimoPorAtraso();
                acrescimoPorAtraso.setEmprestimoId(parcela.getEmprestimoId());
                Optional<Long> maxIdOpt = acrescimoPorAtrasoRepository.findMaxIdByEmprestimoId(parcela.getEmprestimoId());
                Long maxId = maxIdOpt.orElse(0L);
                acrescimoPorAtraso.setId(maxId + 1);
                acrescimoPorAtraso.setValorAtraso(parcela.getValorOriginal().subtract(parcela.getValorPago()));
                acrescimoPorAtraso.setDataRegistro(new Data().toString());
                acrescimoPorAtrasoRepository.save(acrescimoPorAtraso);
            } else {
                gerarNovaParcelaConvencional(emprestimo, parcela.getValorOriginal().subtract(parcela.getValorPago()), parcela);
            }


        }

        // Salva atualização
        parcelaService.save(parcela);

        // Gera nova parcela se aplicável
        EmprestimoEntity emprestimo = buscarEmprestimo(parcela);
        if (emprestimo != null && (parcela.getStatus() == StatusParcela.PAGA.getCode() ||  parcela.getStatus() == StatusParcela.REALOCADO.getCode())) {
            gerarNovaParcelaEspecial(user, emprestimo);
        }

        return true;
    }

    private void gerarNovaParcelaConvencional(EmprestimoEntity emprestimo, BigDecimal valor, ParcelaEntity origem) {
        ParcelaEntity novaParcela = new ParcelaEntity();
        novaParcela.setEmprestimoId(emprestimo.getId());
        novaParcela.setValorOriginal(calculaJuros(valor, emprestimo.getTaxaJurosParcelaAtraso()));
        Data vencimento = new Data();
        vencimento.somarDias(30);
        novaParcela.setVencimento(vencimento.toString());
        novaParcela.setNumeroParcela(buscarQuantidadeParcelas(emprestimo.getId()) + 1);
        novaParcela.setStatus(StatusParcela.PENDENTE.getCode());
        parcelaService.save(novaParcela);

    }

    private Integer buscarQuantidadeParcelas(Long id) {
        return parcelaService.buscarPorEmprestimoId(id).size();
    }

    private BigDecimal calculaJuros(BigDecimal valor, BigDecimal taxaJurosParcelaAtraso) {
        if (valor == null || taxaJurosParcelaAtraso == null) {
            return valor;
        }

        BigDecimal juros = valor.multiply(taxaJurosParcelaAtraso.divide(new BigDecimal(100)));
        return valor.add(juros);
    }


    private void gerarNovaParcelaEspecial(UserEntity user, EmprestimoEntity emprestimo) {
        List<ParcelaEntity> parcelaEntities = parcelaService.buscarPorEmprestimoId(emprestimo.getId());
        BigDecimal saldoDevedor = saldoDevedor(emprestimo);
        if (saldoDevedor.compareTo(BigDecimal.ZERO) <= 0) {
            emprestimo.setDataFechamento(new Data().toString());
            emprestimosService.save(emprestimo);
            return;
        }
        BigDecimal parcelaAtual = CalculadoraJuros.calcularParcela(saldoDevedor, 1, emprestimo.getTaxaJuros(), emprestimo.getTipoEmprestimo());
        novaParcelaEspecial(user, emprestimo, parcelaAtual, parcelaEntities.size() + 1);


    }

    BigDecimal saldoDevedor(EmprestimoEntity emprestimo) {
        List<ParcelaEntity> parcelaEntities = parcelaService.buscarPorEmprestimoId(emprestimo.getId());
        if(emprestimo.getTipoEmprestimo()==TipoEmprestimo.ESPECIAL.getCode()){
            return calculaValorPendenteEspecial(emprestimo,parcelaEntities);
        }else{
            return calculaPendenteNormal(emprestimo,parcelaEntities);

        }
    }

    private BigDecimal calculaPendenteNormal(EmprestimoEntity emprestimo, List<ParcelaEntity> parcelas) {
        BigDecimal valorPendente = BigDecimal.ZERO;

        for (ParcelaEntity parcela : parcelas) {
            BigDecimal pago = parcela.getValorPago() != null ? parcela.getValorPago() : BigDecimal.ZERO;
            BigDecimal desconto = parcela.getValorDesconto() != null ? parcela.getValorDesconto() : BigDecimal.ZERO;
            BigDecimal totalparc = parcela.getValorOriginal() != null ? parcela.getValorOriginal() : BigDecimal.ZERO;

            // valor ainda pendente dessa parcela
            BigDecimal pendenteParcela = totalparc.subtract(pago.add(desconto));

            valorPendente = valorPendente.add(pendenteParcela);
        }

        return valorPendente;
    }

    private BigDecimal calculaValorPendenteEspecial(EmprestimoEntity emprestimo, List<ParcelaEntity> parcelas) {
        BigDecimal valorPendente = emprestimo.getValor();
        BigDecimal valorPendentes = BigDecimal.ZERO;

        for (ParcelaEntity parcela : parcelas) {

            BigDecimal pago = parcela.getValorPago() != null ? parcela.getValorPago() : BigDecimal.ZERO;
            BigDecimal desconto = parcela.getValorDesconto() != null ? parcela.getValorDesconto() : BigDecimal.ZERO;
            BigDecimal totalparc = parcela.getValorOriginal() != null ? parcela.getValorOriginal() : BigDecimal.ZERO;

            // Caso tenha pagado mais do que a parcela vale
            if (totalparc.compareTo(pago.add(desconto)) < 0) {
                BigDecimal pagouMais = pago.add(desconto).subtract(totalparc);
                valorPendente = valorPendente.subtract(pagouMais);
            }

            // Somar parcelas PENDENTES
            if (parcela.getStatus() == StatusParcela.PENDENTE.getCode()) {
                valorPendentes = valorPendentes.add(totalparc);
            }
        }

        BigDecimal totalAtrasos = acrescimoPorAtrasoRepository.findByEmprestimoId(emprestimo.getId())
                .stream()
                .map(a -> a.getValorAtraso() != null ? a.getValorAtraso() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agora sim: soma parcelas pendentes + atrasos
        return valorPendente.add(valorPendentes).add(totalAtrasos);
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
        parcela.setJuros(BigDecimal.ZERO);
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
        return  saldoDevedor(emprestimo);

    }


    // ------------------- Total pago hoje -------------------
    public BigDecimal getTotalPagoNoDia(LocalDate hoje, Long idEmpresa) {
        String dataStr = hoje.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(parcelaService.findByStatus(StatusParcela.PAGA.getCode())
                .stream()
                .filter(p -> p.getDataPagamento() != null && p.getDataPagamento().equals(dataStr))
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .map(p -> p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        total = total.add(parcelaService.findByStatus(StatusParcela.LIQUIDADO.getCode())
                .stream()
                .filter(p -> p.getDataPagamento() != null && p.getDataPagamento().equals(dataStr))
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .map(p -> p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return total;
    }

    // ------------------- Previsão da semana -------------------
    public BigDecimal getPrevisaoSemana(LocalDate hoje, Long idEmpresa) {
        Data dataHoje = new Data(hoje.toString());
        Data dataFinalSemana = new Data(hoje.toString());
        dataFinalSemana.somarSeteDias();

        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .filter(p -> {
                    LocalDate vencimento = new Data(p.getVencimento()).toLocalDate();
                    return !vencimento.isBefore(dataHoje.toLocalDate()) &&
                            !vencimento.isAfter(dataFinalSemana.toLocalDate());
                })
                .map(p -> p.getValorOriginal() != null ? p.getValorOriginal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // ------------------- Quantidade de contratos ativos -------------------
    public long getContratosAtivosCount(Long idEmpresa) {
        List<EmprestimoEntity> emprestimos = emprestimosService.findByEmpresaId(idEmpresa);
        return emprestimos.stream()
                .filter(e -> parcelaService.temParcelasPendentes(e.getId()))
                .count();
    }

    // ------------------- Contratos com parcelas vencidas -------------------
    public long getParcelasVencidasCount(LocalDate hoje, Long idEmpresa) {
        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> LocalDate.parse(p.getVencimento()).isBefore(hoje))
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .map(ParcelaEntity::getEmprestimoId)
                .distinct()
                .count();
    }

    // ------------------- Faturamento previsto para 7 dias -------------------
    public BigDecimal getPrevisaoFaturamentoDias(LocalDate hoje, Long idEmpresa,int diasFuturo) {
        LocalDate semanaFinal = hoje.plusDays(diasFuturo);

        return parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> {
                    LocalDate vencimento = LocalDate.parse(p.getVencimento());
                    return !vencimento.isBefore(hoje) && !vencimento.isAfter(semanaFinal);
                })
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .map(p -> p.getValorOriginal() != null ? p.getValorOriginal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------- Método auxiliar para filtrar parcelas por empresa -------------------
    private boolean pertenceEmpresa(ParcelaEntity parcela, Long idEmpresa) {
        Optional<EmprestimoEntity> emprestimo = emprestimosService.findById(parcela.getEmprestimoId());
        return emprestimo.isPresent() && emprestimo.get().getEmpresaId().equals(idEmpresa);
    }

    public BigDecimal getTotalDinheiroAlocado(Long idEmpresa) {
        // Busca todos os empréstimos da empresa
        List<EmprestimoEntity> emprestimosBase = emprestimosService.findByEmpresaId(idEmpresa);

        List<EmprestimoEntity> emprestimos = emprestimosBase.stream()
                .filter(emprestimo -> parcelaService.temParcelasPendentes(emprestimo.getId()))
                .collect(Collectors.toList());
//        List<EmprestimoEntity> emprestimos = emprestimosService.findByAtivosEmpresaId(idEmpresa);

        // Se não houver empréstimos, retorna zero
        if (emprestimos == null || emprestimos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Soma o valor residual (saldo devedor) de cada empréstimo
        return emprestimos.stream()
                .map(this::calculaValorResidual) // usa o método já existente
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void criarParcelaAdicional(UserEntity userEntity, Long idEmprestimo, BigDecimal valor) throws Exception {
        EmprestimoEntity emprestimo = emprestimosService.findById(idEmprestimo)
                .orElseThrow(() -> new Exception("Empréstimo não encontrado"));

        List<ParcelaEntity> parcelas = parcelaService.findByEmprestimoId(emprestimo.getId());

        // Criar nova parcela
        ParcelaEntity parcela = new ParcelaEntity();
        parcela.setNumeroParcela(parcelas.size()+1);
        parcela.setEmprestimoId(emprestimo.getId());
        parcela.setValorOriginal(BigDecimal.ZERO);
        parcela.setValorPago(valor);
        parcela.setStatus(StatusParcela.PAGA.getCode());
        parcela.setVencimento(new Data().toString());
        parcela.setDataPagamento(new Data().toString());
        parcela.setUsuarioUuid(userEntity.getId());
        parcela.setParcelaAdicional(true);

        // Salvar
        parcelaService.save(parcela);
    }

    public BigDecimal faturadoUltimosDias(LocalDate hoje, Long idEmpresa, int quantidadeDiasAtras) {
        String dataStr = hoje.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(parcelaService.findByStatus(StatusParcela.PAGA.getCode())
                .stream()
                .filter(p -> p.getDataPagamento() != null && p.getDataPagamento().equals(dataStr))
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .map(p -> p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        total = total.add(parcelaService.findByStatus(StatusParcela.LIQUIDADO.getCode())
                .stream()
                .filter(p -> p.getDataPagamento() != null && p.getDataPagamento().equals(dataStr))
                .filter(p -> pertenceEmpresa(p, idEmpresa))
                .map(p -> p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return total;
    }
}


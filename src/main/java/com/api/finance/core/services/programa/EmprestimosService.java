package com.api.finance.core.services.programa;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.EmprestimoDTO;
import com.api.finance.core.dto.ParcelaDTO;
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
import java.time.LocalDate;
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
        emprestimo.setUsuarioId(userGestor.getId());
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
                e.getUsuarioId(), // ou converter UUID se quiser mostrar
                fechamento != null ? fechamento.toLocalDate() : null,
                parcelasService.saldoDevedor(e),
                parcelaService.findByEmprestimoId(e.getId())
                        .stream()
                        .map(p -> toParcelaDTO(p))
                        .collect(Collectors.toList())


        );
    }

    private ParcelaDTO toParcelaDTO(ParcelaEntity parcela) {
        ParcelaDTO parcelaDTO = new ParcelaDTO();
        parcelaDTO.setId(parcelaDTO.getId());
        parcelaDTO.setEmprestimoId(parcelaDTO.getEmprestimoId());
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

    public List<EmprestimoDTO> listarEmprestimosFechadosDTO(UserEntity userGestor) {
        // Obtém todos os empréstimos da empresa do gestor
        List<EmprestimoEntity> todosEmprestimos =
                emprestimoTabelaService.findByEmpresaId(userGestor.getEmpresacliente());

        // Filtra apenas os empréstimos sem parcelas pendentes (fechados)
        List<EmprestimoEntity> emprestimosFechados = todosEmprestimos.stream()
                .filter(e -> !parcelaService.temParcelasPendentes(e.getId()))
                .collect(Collectors.toList());

        // Converte para DTO (usando o mesmo método utilizado nos correntes)
        return emprestimosFechados.stream()
                .sorted(Comparator.comparing(EmprestimoEntity::getId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    public List<EmprestimoDTO> listarEmprestimosComParcelasVencidasDTO(UserEntity userGestor) {
        LocalDate hoje = LocalDate.now();

        // Obtém os IDs dos empréstimos que têm parcelas vencidas
        Set<Long> emprestimosVencidosIds = parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> LocalDate.parse(p.getVencimento()).isBefore(hoje))
                .map(ParcelaEntity::getEmprestimoId)
                .collect(Collectors.toSet());

        // Filtra apenas os empréstimos correntes do gestor que possuem parcelas vencidas
        return listarEmprestimosCorrentesDTO(userGestor)
                .stream()
                .filter(e -> emprestimosVencidosIds.contains(e.getId()))
                .collect(Collectors.toList());
    }

    public void liquidarEmprestimo(UserEntity user, Long emprestimoId, BigDecimal valorPago) {
        // 1️⃣ Busca o empréstimo
        Optional<EmprestimoEntity> emprestimoOpt = emprestimoTabelaService.findById(emprestimoId);
        if (emprestimoOpt.isEmpty()) {
            throw new RuntimeException("Empréstimo não encontrado para liquidação.");
        }
        EmprestimoEntity emprestimo = emprestimoOpt.get();

        // 2️⃣ Busca todas as parcelas vinculadas
        List<ParcelaEntity> parcelas = parcelaService.findByEmprestimoId(emprestimoId);
        if (parcelas.isEmpty()) {
            throw new RuntimeException("Nenhuma parcela encontrada para o empréstimo informado.");
        }

        // 3️⃣ Define data atual
        Data dataAtual = new Data();

        BigDecimal valorParcela = valorPago.divide(new BigDecimal(quantificaParcelasPendentes(parcelas)));

        for (ParcelaEntity parcela : parcelas) {
            if(parcela.getStatus()==StatusParcela.PAGA.getCode())
                continue;

            parcela.setStatus(StatusParcela.LIQUIDADO.getCode());
            parcela.setDataPagamento(dataAtual.toString());
            parcela.setUsuarioUuid(user.getId());
            parcela.setValorPago(valorParcela);

            parcelaService.save(parcela);
        }

        // Atualiza o empréstimo como fechado
        emprestimo.setDataFechamento(dataAtual.toString());
        emprestimoTabelaService.save(emprestimo);
    }

    private Integer quantificaParcelasPendentes(List<ParcelaEntity> parcelas){
        Integer quant = 0;
        for (ParcelaEntity parcela :parcelas){
            if(parcela.getStatus() == StatusParcela.PENDENTE.getCode()){
                quant++;
            }
        }
        return quant;
    }
    public void deletarEmprestimo(UserEntity user, Long emprestimoId) throws Exception {
        // Busca o empréstimo pelo ID
        Optional<EmprestimoEntity> emprestimoOpt = emprestimoTabelaService.findById(emprestimoId);
        if (emprestimoOpt.isEmpty()) {
            throw new Exception("Empréstimo não encontrado para exclusão.");
        }

        EmprestimoEntity emprestimo = emprestimoOpt.get();

        // Apenas o usuário responsável ou gerente pode deletar (opcional)
        if (!user.isGerente() && !user.getId().equals(emprestimo.getUsuarioId())) {
            throw new Exception("Você não tem permissão para deletar este empréstimo.");
        }

        // Marca como deletado
        emprestimo.setDeletado(true);
        emprestimoTabelaService.save(emprestimo);
    }


    // -----------------------------------------------------------
// VERIFICA SE O EMPRÉSTIMO PODE SER EDITADO
// -----------------------------------------------------------
    public boolean podeEditar(Long id) {
        Optional<EmprestimoEntity> emprestimoOpt = emprestimoTabelaService.findById(id);
        if (emprestimoOpt.isEmpty()) {
            return false;
        }

        EmprestimoEntity emprestimo = emprestimoOpt.get();
        List<ParcelaEntity> parcelas = parcelaService.findByEmprestimoId(emprestimo.getId());

        boolean possuiParcelaQuitada = parcelas.stream()
                .anyMatch(p -> p.getStatus() == StatusParcela.PAGA.getCode()
                        || p.getStatus() == StatusParcela.LIQUIDADO.getCode());

        if (!possuiParcelaQuitada) {
            return true;
        }

        return emprestimo.getDataFechamento() == null;
    }

    // -----------------------------------------------------------
// BUSCA UM EMPRÉSTIMO PARA EDIÇÃO
// -----------------------------------------------------------
    public EmprestimoDTO buscarParaEditar(Long id) {
        Optional<EmprestimoEntity> emprestimoOpt = emprestimoTabelaService.findById(id);
        if (emprestimoOpt.isEmpty()) {
            return null;
        }

        EmprestimoEntity e = emprestimoOpt.get();

        // Converte para DTO
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
                e.getUsuarioId(),
                fechamento != null ? fechamento.toLocalDate() : null,
                parcelasService.calculaValorResidual(e),
                new ArrayList<>()
        );
    }

    // -----------------------------------------------------------
    // ATUALIZA UM EMPRÉSTIMO EXISTENTE
    // -----------------------------------------------------------
    public EmprestimoDTO atualizar(UserEntity user, Long id, EmprestimoDTO dto) {
        Optional<EmprestimoEntity> emprestimoOpt = emprestimoTabelaService.findById(id);
        if (emprestimoOpt.isEmpty()) {
            return null;
        }

        EmprestimoEntity emprestimo = emprestimoOpt.get();

        // Atualiza os campos editáveis
        emprestimo.setCliente(dto.getCliente());
        emprestimo.setContato(dto.getContato());
        emprestimo.setId(dto.getId());
        emprestimo.setValor(dto.getValor());
        emprestimo.setTipoEmprestimo(dto.getTipoEmprestimo());
        emprestimo.setTaxaJuros(dto.getTaxaJuros());
        emprestimo.setQuantidadeParcelas(dto.getQuantidadeParcelas());
        emprestimo.setVencimentoPrimeiraParcela(dto.getVencimentoPrimeiraParcela() != null ? dto.getVencimentoPrimeiraParcela().toString() : null);
        emprestimo.setTipoCobranca(dto.getTipoCobranca());

        emprestimoTabelaService.save(emprestimo);

        if (podeEditar(id)) {
            removerParcelas(dto.getId());
            gerarParcelas(user, emprestimo);
        }

        // Retorna DTO atualizado
        return buscarParaEditar(id);
    }

    private void removerParcelas(Long id) {
        parcelaService.findByEmprestimoId(id).forEach(parcela -> {
            parcelaService.remove(parcela.getId());
        });
    }

    public EmprestimoDTO findEmprestimo(Long id) {
        return toDTO(findbyId(id));

    }
    public EmprestimoEntity findbyId(Long id) {
        return emprestimoTabelaService.findById(id)
                .orElseThrow(() -> new RuntimeException("Empréstimo não encontrado: " + id));
    }

    public List<EmprestimoDTO> listarEmprestimosVenceHoje(UserEntity userGestor) {

        LocalDate hoje = LocalDate.now();

        // Obtém os IDs dos empréstimos que têm parcelas vencidas
        Set<Long> emprestimosVencidosIds = parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> LocalDate.parse(p.getVencimento()).equals(hoje))
                .map(ParcelaEntity::getEmprestimoId)
                .collect(Collectors.toSet());

        // Filtra apenas os empréstimos correntes do gestor que possuem parcelas vencidas
        return listarEmprestimosCorrentesDTO(userGestor)
                .stream()
                .filter(e -> emprestimosVencidosIds.contains(e.getId()))
                .collect(Collectors.toList());

    }

    public List<EmprestimoDTO> listarEmprestimosPagoHoje(UserEntity userGestor) {
        LocalDate hoje = LocalDate.now();

        // Obtém os IDs dos empréstimos que têm parcelas vencidas
        Set<Long> emprestimosVencidosIds = parcelaService.findByStatus(StatusParcela.PENDENTE.getCode())
                .stream()
                .filter(p -> LocalDate.parse(p.getVencimento()).equals(hoje))
                .map(ParcelaEntity::getEmprestimoId)
                .collect(Collectors.toSet());

        // Filtra apenas os empréstimos correntes do gestor que possuem parcelas vencidas
        return listarEmprestimosCorrentesDTO(userGestor)
                .stream()
                .filter(e -> emprestimosVencidosIds.contains(e.getId()))
                .collect(Collectors.toList());


    }
}

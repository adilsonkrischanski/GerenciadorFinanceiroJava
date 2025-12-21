package com.api.finance.core.services.tabela;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.ParcelaDTO;
import com.api.finance.core.repositories.ParcelaRepository;
import com.api.finance.core.services.sistema.Data;
import com.api.finance.core.utils.enums.StatusParcela;
import com.api.finance.core.utils.enums.TipoEmprestimo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ParcelaService {

    @Autowired
    private ParcelaRepository parcelaRepository;

    /**
     * Salva uma parcela no banco de dados.
     */
    public ParcelaEntity save(ParcelaEntity table) {
        return parcelaRepository.save(table);
    }

    /**
     * Busca todas as parcelas de um empréstimo.
     */
    public List<ParcelaEntity> buscarPorEmprestimo(Long emprestimoId) {
        return parcelaRepository.findByEmprestimoId(emprestimoId);
    }

    /**
     * Busca todas as parcelas pendentes.
     */
    public List<ParcelaEntity> buscarPendentes() {
        return parcelaRepository.findByStatus(StatusParcela.PENDENTE.getCode());
    }

    /**
     * Atualiza o status de uma parcela.
     */
    public ParcelaEntity atualizarStatus(Long id, StatusParcela novoStatus) {
        ParcelaEntity parcela = parcelaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parcela não encontrada com ID: " + id));
        parcela.setStatus(novoStatus.getCode());
        return parcelaRepository.save(parcela);
    }


    public List<ParcelaEntity> buscarPorEmprestimoId(Long emprestimoId) {
        return parcelaRepository.findByEmprestimoId(emprestimoId);
    }

    public ParcelaEntity buscarPorId(Long id) {
        return parcelaRepository.findById(id).orElse(null);
    }


    public List<ParcelaEntity> findByEmprestimoId(Long emprestimoId) {
        return parcelaRepository.findByEmprestimoId(emprestimoId);
    }

    public ParcelaEntity findById(Long id) {
        return parcelaRepository.findById(id).orElse(null);
    }

    public boolean temParcelasPendentes(Long emprestimoId) {
        return parcelaRepository.existsByEmprestimoIdAndStatus(emprestimoId, StatusParcela.PENDENTE.getCode());
    }
    public List<ParcelaEntity> findByStatus(int status) {
        return parcelaRepository.findByStatus(status);
    }


    public boolean remove(Long id) {
        ParcelaEntity parcela = parcelaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parcela não encontrada com ID: " + id));

        parcelaRepository.delete(parcela);
        return true;
    }

    public ParcelaEntity proximaParcela(Long emprestimo) {

        Optional<ParcelaEntity> parcela = Optional.ofNullable(parcelaRepository.findFirstByEmprestimoIdAndStatusOrderByNumeroParcelaAsc(
                emprestimo, StatusParcela.PENDENTE.getCode()));

        return parcela.get();
    }
}

package com.api.finance.core.repositories;

import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParcelaRepository extends JpaRepository<ParcelaEntity, Long> {

    /**
     * Busca todas as parcelas de um determinado empréstimo.
     * @param emprestimoId ID do empréstimo
     * @return lista de parcelas
     */
    List<ParcelaEntity> findByEmprestimoId(Long emprestimoId);

    /**
     * Busca todas as parcelas com determinado status.
     * @param status código do status (ex: 1 = paga, 0 = pendente)
     * @return lista de parcelas
     */
    List<ParcelaEntity> findByStatus(int status);

    /**
     * Busca uma parcela específica pelo número e empréstimo.
     * @param emprestimoId ID do empréstimo
     * @param numeroParcela número da parcela
     * @return parcela específica (ou null se não existir)
     */
    ParcelaEntity findByEmprestimoIdAndNumeroParcela(Long emprestimoId, Integer numeroParcela);

    boolean existsByEmprestimoIdAndStatus(Long emprestimoId, int status);

    ParcelaEntity findFirstByEmprestimoIdAndStatusOrderByNumeroParcelaAsc(
            Long emprestimoId,
            int status
    );
}

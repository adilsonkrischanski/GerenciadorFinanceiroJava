package com.api.finance.core.repositories;

import com.api.finance.core.domain.entity.AcrescimoPorAtraso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcrescimoPorAtrasoRepository extends JpaRepository<AcrescimoPorAtraso, Long> {

    /**
     * Busca todos os acréscimos de atraso de um determinado empréstimo.
     * @param emprestimoId ID do empréstimo
     * @return lista de acréscimos
     */
    List<AcrescimoPorAtraso> findByEmprestimoId(Long emprestimoId);

    /**
     * Verifica se já existe algum acréscimo para um empréstimo específico.
     * @param emprestimoId ID do empréstimo
     * @return true se existir pelo menos um registro
     */
    boolean existsByEmprestimoId(Long emprestimoId);

    @Query("SELECT MAX(a.id) FROM AcrescimoPorAtraso a WHERE a.emprestimoId = :emprestimoId")
    Optional<Long> findMaxIdByEmprestimoId(@Param("emprestimoId") Long emprestimoId);
}

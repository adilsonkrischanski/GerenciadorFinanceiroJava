package com.api.finance.core.repositories;

import com.api.finance.core.domain.entity.EmprestimoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmprestimoRepository extends JpaRepository<EmprestimoEntity, Long> {

    // Busca todos os empréstimos de um cliente específico
    List<EmprestimoEntity> findAllByCliente(String cliente);

    // Busca por ID (já existe em JpaRepository, mas manter é ok)
    Optional<EmprestimoEntity> findById(Long id);

    // Busca todos os empréstimos de um usuário (gerente) específico
    List<EmprestimoEntity> findByUsuarioId(UUID usuarioId);

    // Busca todos os empréstimos de uma empresa específica
    List<EmprestimoEntity> findByEmpresaId(Long empresaId);
}

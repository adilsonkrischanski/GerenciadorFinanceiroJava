package com.api.finance.core.repositories;

import com.api.finance.auth.domain.user.EmpresaClienteSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaClienteSequenceRepository extends JpaRepository<EmpresaClienteSequence, Long> {

}

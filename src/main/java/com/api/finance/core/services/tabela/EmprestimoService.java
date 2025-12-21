package com.api.finance.core.services.tabela;

import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.repositories.EmprestimoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmprestimoService {

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    public EmprestimoEntity save(EmprestimoEntity table) {
        return emprestimoRepository.save(table);
    }

    public void deleteById(Long id) {
        emprestimoRepository.deleteById(id);
    }

    public EmprestimoEntity update(EmprestimoEntity table) {
        return emprestimoRepository.save(table);
    }

    public Optional<EmprestimoEntity> findById(Long id) {
        return emprestimoRepository.findById(id);
    }

    public List<EmprestimoEntity> findByEmpresaId(Long empresaId) {
        return emprestimoRepository.findByEmpresaId(empresaId);
    }

    public List<EmprestimoEntity> findByAtivosEmpresaId(Long empresaId) {
        return emprestimoRepository.findByEmpresaId(empresaId);
    }





}

package com.api.finance.core.controllers;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.auth.domain.user.UserSecurity;
import com.api.finance.auth.service.UserService;

import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.EmprestimoDTO;


import com.api.finance.core.services.programa.EmprestimosService;
import com.api.finance.core.services.programa.ParcelasService;
import com.api.finance.core.services.sistema.Data;
import com.api.finance.core.utils.enums.StatusParcela;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/emprestimo")
public class EmprestimoController {

    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EmprestimosService emprestimoService;

    @Autowired
    ParcelasService parcelaService;



    @GetMapping("/test-integracao")
    public ResponseEntity<Map<String, UUID>> testIntegration(@AuthenticationPrincipal UserSecurity userSecurity) {
        if (userSecurity == null) {
            return null; // ou ResponseEntity.status(401).build()
        }
        Map<String, UUID> response = new HashMap<>();
        response.put("result", userSecurity.getUser());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@AuthenticationPrincipal UserSecurity userSecurity,
                                                        @RequestBody EmprestimoDTO body) throws Exception {

        if (userSecurity == null) {
            return null; // ou ResponseEntity.status(401).build()
        }
        userSecurity.getUser();

        Optional<UserEntity> userGestorOptional = userService.findById(userSecurity.getUser());

        UserEntity userGestor = userGestorOptional.get();
        if (!userGestor.isGerente()) {
            throw new Exception("Voce precisar estar logado como ser o responsavel para cadastrar um emprestimo");
        }
        return emprestimoService.criarEmprestimo(userGestor, body);


    }

    @GetMapping("/list")
    public ResponseEntity<List<EmprestimoDTO>> listarEmprestimos(
            @AuthenticationPrincipal UserSecurity userSecurity,
            @RequestParam(value = "status", required = false, defaultValue = "aberto") String status) {

        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<UserEntity> userGestorOptional = userService.findById(userSecurity.getUser());
        if (!userGestorOptional.isPresent()) {
            return ResponseEntity.status(404).build();
        }

        UserEntity userGestor = userGestorOptional.get();
        List<EmprestimoDTO> emprestimos;

        switch (status.toLowerCase()) {
            case "fechado":
                emprestimos = emprestimoService.listarEmprestimosFechadosDTO(userGestor);
                break;
            case "vencido":
                emprestimos = emprestimoService.listarEmprestimosComParcelasVencidasDTO(userGestor);
                break;
            default:
                emprestimos = emprestimoService.listarEmprestimosCorrentesDTO(userGestor);
                break;
        }

        return ResponseEntity.ok(emprestimos);
    }


    @PostMapping("liquidar/{id}")
    public ResponseEntity<String> liquidar(
            @AuthenticationPrincipal UserSecurity userSecurity,
            @PathVariable("id") Long emprestimoId,
            @RequestBody Map<String, BigDecimal> payload) {

        BigDecimal valorPago = payload.get("valorPago");

        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<UserEntity> userOpt = userService.findById(userSecurity.getUser());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        try {
            emprestimoService.liquidarEmprestimo(userOpt.get(), emprestimoId,valorPago);
            return ResponseEntity.ok("Empréstimo liquidado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deletarEmprestimo(
            @AuthenticationPrincipal UserSecurity userSecurity,
            @PathVariable("id") Long emprestimoId) {

        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<UserEntity> userOpt = userService.findById(userSecurity.getUser());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        try {
            emprestimoService.deletarEmprestimo(userOpt.get(), emprestimoId);
            Map<String, String> response = new HashMap<>();
            response.put("result", "Empréstimo deletado com sucesso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }


    }

    // ✅ 1. Verifica se é possível editar
    @GetMapping("/pode-editar/{id}")
    public ResponseEntity<Boolean> podeEditar(@AuthenticationPrincipal UserSecurity userSecurity,
                                              @PathVariable Long id) {
        boolean podeEditar = emprestimoService.podeEditar(id);
        return ResponseEntity.ok(podeEditar);
    }

    // ✅ 2. Busca o objeto para edição
    @GetMapping("/editar/{id}")
    public ResponseEntity<EmprestimoDTO> buscarParaEditar(@AuthenticationPrincipal UserSecurity userSecurity,
                                                          @PathVariable Long id) {
        EmprestimoDTO dto = emprestimoService.buscarParaEditar(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<EmprestimoDTO> atualizar(@AuthenticationPrincipal UserSecurity userSecurity,
                                                   @PathVariable Long id, @RequestBody EmprestimoDTO dto) {

        Optional<UserEntity> userOpt = userService.findById(userSecurity.getUser());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(new EmprestimoDTO());
        }

        EmprestimoDTO atualizado = emprestimoService.atualizar(id, dto);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(atualizado);
    }

}




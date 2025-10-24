package com.api.finance.core.controllers;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.auth.domain.user.UserSecurity;
import com.api.finance.auth.service.UserService;
import com.api.finance.core.services.programa.ParcelasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/dash")
public class DashController {

    @Autowired
    private ParcelasService parcelasService;

    @Autowired
    private UserService userService;

    // ----------------- Total pago hoje -----------------
    @GetMapping("/pago-hoje")
    public ResponseEntity<BigDecimal> getPagoHoje(@AuthenticationPrincipal UserSecurity userSecurity) {
        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        LocalDate hoje = LocalDate.now();
        BigDecimal totalPago = parcelasService.getTotalPagoNoDia(hoje);
        return ResponseEntity.ok(totalPago);
    }

    // ----------------- Previsão da semana -----------------
    @GetMapping("/previsao-semana")
    public ResponseEntity<BigDecimal> getPrevisaoSemana(@AuthenticationPrincipal UserSecurity userSecurity) {
        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        LocalDate hoje = LocalDate.now();
        BigDecimal previsao = parcelasService.getPrevisaoSemana(hoje);
        return ResponseEntity.ok(previsao);
    }

    // ----------------- Contratos ativos -----------------
    @GetMapping("/contratos-ativos")
    public ResponseEntity<Long> getContratosAtivos(@AuthenticationPrincipal UserSecurity userSecurity) {
        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<UserEntity> userOpt = userService.findById(userSecurity.getUser());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        Long idEmpresa = userOpt.get().getEmpresacliente(); // id da empresa do usuário
        long total = parcelasService.getContratosAtivosCount(idEmpresa);
        return ResponseEntity.ok(total);
    }

    // ----------------- Parcelas vencidas -----------------
    @GetMapping("/parcelas-vencidas")
    public ResponseEntity<Long> getParcelasVencidas(@AuthenticationPrincipal UserSecurity userSecurity) {
        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        LocalDate hoje = LocalDate.now();
        long totalVencidas = parcelasService.getParcelasVencidasCount(hoje);
        return ResponseEntity.ok(totalVencidas);
    }

    // ----------------- Faturamento previsto 7 dias -----------------
    @GetMapping("/faturamento-7-dias")
    public ResponseEntity<BigDecimal> getFaturamento7Dias(@AuthenticationPrincipal UserSecurity userSecurity) {
        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        LocalDate hoje = LocalDate.now();
        BigDecimal faturamento = parcelasService.getFaturamento7Dias(hoje);
        return ResponseEntity.ok(faturamento);
    }
}

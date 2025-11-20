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
import java.util.HashMap;
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
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal totalPago = parcelasService.getTotalPagoNoDia(LocalDate.now(), idEmpresa);
        return ResponseEntity.ok(totalPago);
    }

    // ----------------- Previsão da semana -----------------
    @GetMapping("/previsao-semana")
    public ResponseEntity<BigDecimal> getPrevisaoSemana(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal previsao = parcelasService.getPrevisaoSemana(LocalDate.now(), idEmpresa);
        return ResponseEntity.ok(previsao);
    }

    // ----------------- Previsão 15 dias -----------------
    @GetMapping("/previsao-15-dias")
    public ResponseEntity<BigDecimal> getPrevisao15Dias(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal previsao = parcelasService.getPrevisaoFaturamentoDias(LocalDate.now(), idEmpresa,15);
        return ResponseEntity.ok(previsao);
    }

    // ----------------- Previsão 30 dias -----------------
    @GetMapping("/previsao-30-dias")
    public ResponseEntity<BigDecimal> getPrevisao30Dias(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal previsao = parcelasService.getPrevisaoFaturamentoDias(LocalDate.now(), idEmpresa,30);
        return ResponseEntity.ok(previsao);
    }

    // ----------------- Contratos ativos -----------------
    @GetMapping("/contratos-ativos")
    public ResponseEntity<Long> getContratosAtivos(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        long total = parcelasService.getContratosAtivosCount(idEmpresa);
        return ResponseEntity.ok(total);
    }

    // ----------------- Parcelas vencidas -----------------
    @GetMapping("/parcelas-vencidas")
    public ResponseEntity<Long> getParcelasVencidas(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        long totalVencidas = parcelasService.getParcelasVencidasCount(LocalDate.now(), idEmpresa);
        return ResponseEntity.ok(totalVencidas);
    }

    // ----------------- Faturamento previsto próximos 7 dias -----------------
    @GetMapping("/faturamento-7-dias")
    public ResponseEntity<BigDecimal> getFaturamento7Dias(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal faturamento = parcelasService.getPrevisaoFaturamentoDias(LocalDate.now(), idEmpresa,7);
        return ResponseEntity.ok(faturamento);
    }

    // ----------------- Faturamento últimos 7 dias -----------------
    @GetMapping("/faturamento-7-dias-passados")
    public ResponseEntity<BigDecimal> getFaturamento7DiasPassados(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal faturamento = parcelasService.getPrevisaoFaturamentoDias(LocalDate.now(), idEmpresa,7);
        return ResponseEntity.ok(faturamento);
    }

    // ----------------- Total de dinheiro alocado -----------------
    @GetMapping("/total-alocado")
    public ResponseEntity<BigDecimal> getTotalAlocado(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        BigDecimal total = parcelasService.getTotalDinheiroAlocado(idEmpresa);
        return ResponseEntity.ok(total);
    }

    // ----------------- Resumo completo -----------------
    @GetMapping("/resumo")
    public ResponseEntity<?> getResumo(@AuthenticationPrincipal UserSecurity userSecurity) {
        Optional<UserEntity> userOpt = validarUser(userSecurity);
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();

        Long idEmpresa = userOpt.get().getEmpresacliente();
        LocalDate hoje = LocalDate.now();



        HashMap<String, Object> resumo = new HashMap<>();
        resumo.put("pagoHoje", parcelasService.getTotalPagoNoDia(hoje, idEmpresa));
        resumo.put("previsaoSemana", parcelasService.getPrevisaoSemana(hoje, idEmpresa));
        resumo.put("previsao15Dias", parcelasService.getPrevisaoFaturamentoDias(hoje, idEmpresa,15));
        resumo.put("previsao30Dias", parcelasService.getPrevisaoFaturamentoDias(hoje, idEmpresa,30));
        resumo.put("parcelasVencidas", parcelasService.getParcelasVencidasCount(hoje, idEmpresa));
        resumo.put("contratosAtivos", parcelasService.getContratosAtivosCount(idEmpresa));
        resumo.put("faturamento7Dias", parcelasService.getPrevisaoFaturamentoDias(hoje, idEmpresa,7));
        resumo.put("totalAlocado", parcelasService.getTotalDinheiroAlocado(idEmpresa));

        return ResponseEntity.ok(resumo);
    }

    // ----------------- Método auxiliar -----------------
    private Optional<UserEntity> validarUser(UserSecurity userSecurity) {
        if (userSecurity == null) return Optional.empty();
        return userService.findById(userSecurity.getUser());
    }
}

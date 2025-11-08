package com.api.finance.core.controllers;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.auth.domain.user.UserSecurity;
import com.api.finance.auth.service.UserService;
import com.api.finance.core.domain.entity.EmprestimoEntity;
import com.api.finance.core.domain.entity.ParcelaEntity;
import com.api.finance.core.dto.ParcelaDTO;

import com.api.finance.core.services.programa.ParcelasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/parcela")
public class ParcelaController {

    @Autowired
    private ParcelasService parcelaService;

    @Autowired
    UserService userService;

    /**
     * Retorna todas as parcelas associadas a um empréstimo específico.
     * Exemplo de endpoint: GET /parcela/emprestimo/10
     */
    @GetMapping("/emprestimo/{emprestimoId}")
    public ResponseEntity<List<ParcelaEntity>> listarParcelasPorEmprestimo(@PathVariable Long emprestimoId) {
        List<ParcelaEntity> parcelas = parcelaService.buscarPorEmprestimoId(emprestimoId);

        if (parcelas == null || parcelas.isEmpty()) {
            return ResponseEntity.noContent().build(); // HTTP 204 - sem conteúdo
        }

        List<ParcelaEntity> ordenados = parcelas.stream()
                .sorted(Comparator.comparing(ParcelaEntity::getId))
                .toList();

        return ResponseEntity.ok(parcelas); // HTTP 200 - lista de parcelas
    }

    /**
     * Retorna uma parcela específica pelo ID.
     * Exemplo de endpoint: GET /parcela/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParcelaDTO> buscarParcelaPorId(@PathVariable Long id) {
        ParcelaDTO parcela = parcelaService.buscarDtoPorId(id);

        if (parcela == null) {
            return ResponseEntity.notFound().build(); // HTTP 404 - não encontrada
        }

        return ResponseEntity.ok(parcela); // HTTP 200 - encontrada
    }

    @PostMapping("/pagar")
    public ResponseEntity<Map<String, Object>> confirmarPagamento(
            @AuthenticationPrincipal UserSecurity userSecurity,
            @RequestBody ParcelaDTO dto) throws Exception {

        // Verifica se o usuário está autenticado
        if (userSecurity == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "ERRO",
                    "mensagem", "Usuário não autenticado."
            ));
        }

        Optional<UserEntity> userGestorOptional = userService.findById(userSecurity.getUser());
        if (userGestorOptional.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "ERRO",
                    "mensagem", "Usuário precisa estar logado."
            ));
        }

        UserEntity userGestor = userGestorOptional.get();

        try {
            boolean sucesso = parcelaService.confirmarPagamento(userGestor, dto);

            if (!sucesso) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERRO",
                        "mensagem", "Não foi possível confirmar o pagamento."
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "mensagem", "Pagamento confirmado com sucesso!"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERRO",
                    "mensagem", "Erro ao confirmar pagamento: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/adicional")
    public ResponseEntity<Map<String, Object>> criarParcelaAdicional(
            @AuthenticationPrincipal UserSecurity userSecurity,
            @RequestBody Map<String, Object> payload) {

        if (userSecurity == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "ERRO",
                    "mensagem", "Usuário não autenticado."
            ));
        }

        Optional<UserEntity> userOpt = userService.findById(userSecurity.getUser());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "ERRO",
                    "mensagem", "Usuário não encontrado"
            ));
        }

        Long idEmprestimo = ((Number) payload.get("idEmprestimo")).longValue();
        Double valorDouble = ((Number) payload.get("valor")).doubleValue();
        BigDecimal valor = BigDecimal.valueOf(valorDouble);

        try {
            parcelaService.criarParcelaAdicional(userOpt.get(), idEmprestimo, valor);
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "mensagem", "Parcela adicional criada com sucesso!"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of(
                    "status", "ERRO",
                    "mensagem", "Erro ao criar parcela adicional: " + e.getMessage()
            ));
        }
    }


}

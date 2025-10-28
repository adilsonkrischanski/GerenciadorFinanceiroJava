package com.api.finance.core.controllers;

import com.api.finance.auth.domain.user.EmpresaClienteSequence;
import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.auth.domain.user.UserSecurity;
import com.api.finance.auth.dto.RegisterDTO;
import com.api.finance.auth.service.UserService;
import com.api.finance.core.repositories.EmpresaClienteSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/cliente")
public class ClienteController {

    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EmpresaClienteSequenceRepository empresaClienteSequence;



    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@AuthenticationPrincipal UserSecurity userSecurity,
                                                        @RequestBody RegisterDTO body) throws Exception {

        if (userSecurity == null) {
            return null; // ou ResponseEntity.status(401).build()
        }
        userSecurity.getUser();

        Optional<UserEntity> userGestorOptional = userService.findById(userSecurity.getUser());
        if (userGestorOptional.isEmpty()) {
            throw new Exception("Voce precisar estar logado como administrador para cadastrar um funcionario");
        }
        UserEntity userGestor = userGestorOptional.get();


        Optional<UserEntity> userEntity = userService.findByEmail(body.email());

        if (userEntity.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(body.password());
            UserEntity newUser = new UserEntity();
            newUser.setEmail(body.email());
            newUser.setUsername(body.username());
            newUser.setPassword(encodedPassword);
            newUser.setGerente(body.isGerente());
            newUser.setAdministrator(body.isAdministrator());
            newUser.setRegistrationDate(LocalDateTime.now());
            EmpresaClienteSequence seq = empresaClienteSequence.save(new EmpresaClienteSequence());
            newUser.setEmpresacliente(seq.getId());

            newUser.setActive(true);
            newUser.setDeactivationDate(null);
            userService.save(newUser);
            Map<String, String> response = new HashMap<>();
            response.put("result", "Usuario cadastrado!");
            return ResponseEntity.ok(response);
        } else {
            throw new Exception("Email ja cadastrado.");
        }
    }



    // 🔹 Listar apenas email e ID dos funcionários da mesma empresa do gerente
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listarColaboradores(@AuthenticationPrincipal UserSecurity userSecurity) throws Exception {
        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<UserEntity> userOptional = userService.findById(userSecurity.getUser());
        if (userOptional.isEmpty() || !userOptional.get().isGerente()) {
            throw new Exception("Acesso restrito a gerentes.");
        }

        UserEntity gerente = userOptional.get();
        List<UserEntity> funcionarios = userService.findAllByEmpresa(gerente.getEmpresacliente());

        List<Map<String, Object>> lista = funcionarios.stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("email", u.getEmail());
                    map.put("ativo", u.isActive());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    // 🔹 Desativar usuário (por ID)
    @PutMapping("/desativar/{id}")
    public ResponseEntity<Map<String, String>> desativarUsuario(@AuthenticationPrincipal UserSecurity userSecurity,
                                                                @PathVariable UUID id) throws Exception {

        if (userSecurity == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<UserEntity> userOptional = userService.findById(userSecurity.getUser());
        if (userOptional.isEmpty() || !userOptional.get().isGerente()) {
            throw new Exception("Acesso restrito a gerentes.");
        }

        Optional<UserEntity> userTargetOpt = userService.findById(id);
        if (userTargetOpt.isEmpty()) {
            throw new Exception("Usuário não encontrado.");
        }

        UserEntity userTarget = userTargetOpt.get();
        userTarget.setActive(false);
        userTarget.setDeactivationDate(LocalDateTime.now());
        userService.save(userTarget);

        Map<String, String> response = new HashMap<>();
        response.put("result", "Usuário desativado com sucesso.");
        return ResponseEntity.ok(response);
    }
}

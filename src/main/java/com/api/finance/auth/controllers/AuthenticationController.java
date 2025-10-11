package com.api.finance.auth.controllers;

import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.auth.dto.LoginDTO;
import com.api.finance.auth.dto.RegisterDTO;
import com.api.finance.auth.dto.ReveryPasswordDTO;
import com.api.finance.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthenticationController {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserService userService;

    @GetMapping("/")
    public String responseTest(HttpServletRequest httpServletRequest) {
        return String.format("Welcome! Your session Id: %s", httpServletRequest.getSession().getId());
    }

    @CrossOrigin("*")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterDTO body) {Optional<UserEntity> userEntity = userService.findByEmail(body.email());

        if (userEntity.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(body.password());
            UserEntity newUser = new UserEntity();
            newUser.setEmail(body.email());
            newUser.setUsername(body.username());
            newUser.setPassword(encodedPassword);
            newUser.setRegistrationDate(LocalDateTime.now());
            newUser.setActive(true);
            newUser.setDeactivationDate(null);
            newUser.setAdministrator(false);
            userService.save(newUser);
            return gererateToken(new LoginDTO(body.email(),body.password()));
        }

        return ResponseEntity.badRequest().build();
    }

    @CrossOrigin("*")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginDTO body) {
        return gererateToken(body);
    }

    @CrossOrigin("*")
    @PostMapping("/reset-password-send-mail")
    public ResponseEntity<Map<String, Boolean>> resetPasswordSendMail(@RequestBody LoginDTO body){
        Map<String, Boolean> response = new HashMap<>();
        response.put("result",userService.sendCodeResetPassword(body.email()));

        return ResponseEntity.ok(response);
    }


    @CrossOrigin("*")
    @PostMapping("/confirm-reset-password")
    public ResponseEntity<Map<String, Boolean>> confirmCodeResetPassWord(@RequestBody ReveryPasswordDTO body){
        Map<String, Boolean> response = new HashMap<>();
        response.put("result",userService.ResetPassword(body));

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> gererateToken(LoginDTO body){
    String jwtToken = userService.verify(body);
    Map<String, String> response = new HashMap<>();
        response.put("token", jwtToken);
        return ResponseEntity.ok(response);
    }

    @CrossOrigin("*")
    @GetMapping("/test-integracao")
    public Integer TestaIntegra(){
        return 1;
    }




}

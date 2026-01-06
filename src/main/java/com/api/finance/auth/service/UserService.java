package com.api.finance.auth.service;

import com.api.finance.auth.domain.user.EmpresaClienteSequence;
import com.api.finance.auth.domain.user.UserEntity;
import com.api.finance.auth.dto.AlterarSenhaDTO;
import com.api.finance.auth.dto.LoginDTO;
import com.api.finance.auth.dto.ReveryPasswordDTO;
import com.api.finance.auth.repositories.UserRepository;
import com.api.finance.auth.service.security.TokenService;
import com.api.finance.core.repositories.EmpresaClienteSequenceRepository;
import com.api.finance.core.services.sistema.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    TokenService tokenService;
    @Autowired
    EmailService emailService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EmpresaClienteSequenceRepository empresaClienteSequenceRepository;

    public UserEntity save(UserEntity user) {
        if (user.getEmpresacliente() == 0) {
            // Cria um novo registro na tabela de sequência
            EmpresaClienteSequence seq = empresaClienteSequenceRepository.save(new EmpresaClienteSequence());
            user.setEmpresacliente(seq.getId());
        }

        return userRepository.save(user);

    }

    public void deleteById(UUID id) {
        userRepository.deleteById(id);
    }

    public UserEntity update(UserEntity user) {
        return userRepository.save(user);
    }

    public Optional<UserEntity> findByEmail(String username) {
        return userRepository.findByEmail(username);
    }
    public Optional<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }

    public String verify(LoginDTO userLoginData) {
        Authentication authUser = new UsernamePasswordAuthenticationToken(userLoginData.email(), userLoginData.password());
        Authentication auth = authenticationManager.authenticate(authUser);

        if (auth.isAuthenticated()) {
            Optional<UserEntity> userEntityOptional = findByEmail(userLoginData.email());

            if (userEntityOptional.isPresent()) {
                return tokenService.generateToken(userEntityOptional.get());
            }
        }

        return null;
    }

    public Boolean verificaSeGerente(String email){

        try {
            Optional<UserEntity> userEntityOptional = userRepository.findByEmail(email);
            if(userEntityOptional.isPresent()) {
                UserEntity user = userEntityOptional.get();
                return user.isGerente();
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    public Boolean verificaSeAdministrador(String email){
        try {
            Optional<UserEntity> userEntityOptional = userRepository.findByEmail(email);
            if(userEntityOptional.isPresent()) {
                UserEntity user = userEntityOptional.get();
                return user.isAdministrator();
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    public Boolean sendCodeResetPassword(String email){
        try {
            emailService.sendEmail(email,"Recuperação de senha","Seu codigo é: 1234");
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public Boolean verifyCodeRecovery(ReveryPasswordDTO body) {

        //Implementar verificação de codigo
        return true;
    }

    public void resetPassWord(UserEntity user, AlterarSenhaDTO alterarSenhaDTO){
        String encodedPassword = passwordEncoder.encode(alterarSenhaDTO.getNovaSenha());
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    public void resetPassWordPadrao(UserEntity user){
        String encodedPassword = passwordEncoder.encode(user.getEmail());
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    public void resetPassWord(ReveryPasswordDTO reveryPasswordDTO){
        UserEntity user = userRepository.findByEmail(reveryPasswordDTO.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        String encodedPassword = passwordEncoder.encode(reveryPasswordDTO.newPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);

    }


    public Boolean ResetPassword(ReveryPasswordDTO body) {
        try {
            verifyCodeRecovery(body) ;
            emailService.sendEmail(body.email(), "Recuperação de senha", "Sua senha foi alterada");
            resetPassWord(body);

            return true;
        }catch (Exception e){
            return false;
        }
    }

    public List<UserEntity> findAllByEmpresa(long empresacliente) {
        return userRepository.findAllByEmpresacliente(empresacliente);
    }

    public boolean ativarColaborador(UUID id) {
        Optional<UserEntity> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            UserEntity user = opt.get();
            user.setDeactivationDate(null);
            user.setActive(true);
            user.setPassword(passwordEncoder.encode(user.getEmail()));
            userRepository.save(user);
            return true;
        }
        return false;
    }

}

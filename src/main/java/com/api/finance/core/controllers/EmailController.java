package com.api.finance.core.controllers;

import com.api.finance.core.services.sistema.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    @Autowired
    private EmailService emailService;

    @CrossOrigin("*")
    @GetMapping("/send-mail")
    public String send(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String message) {

        emailService.sendEmail(to, subject, message);
        return "E-mail send with sucess to : " + to;
    }
}

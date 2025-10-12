package com.api.finance.core.controllers;

import jakarta.annotation.security.PermitAll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    // Redireciona apenas rotas do front-end (Angular)
    @GetMapping(value = {
            "/",                        // rota raiz
            "/home",                    // rota específica
            "/dashboard/**",            // qualquer subrota do Angular
            "/profile/**"               // outra subrota do Angular
    })
    public String redirect() {
        return "forward:/index.html";
    }
}

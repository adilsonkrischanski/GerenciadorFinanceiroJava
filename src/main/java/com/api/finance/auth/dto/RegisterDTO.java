package com.api.finance.auth.dto;

public record RegisterDTO(
        String username,
        String email,
        String password,
        String contato,
        boolean isAdministrator,
        boolean isGerente
){}

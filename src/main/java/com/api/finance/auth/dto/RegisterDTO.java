package com.api.finance.auth.dto;

public record RegisterDTO(
        String username,
        String email,
        String password
) {}

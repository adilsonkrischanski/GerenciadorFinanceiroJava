package com.api.finance.auth.dto;

public record ReveryPasswordDTO(
        String code,
        String newPassword,
        String email
) {}

package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenDto {
    @NotBlank(message = "Refresh token bo'sh bo'lishi mumkin emas")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

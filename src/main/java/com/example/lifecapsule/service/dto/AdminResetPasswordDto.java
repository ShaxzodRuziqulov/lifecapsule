package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminResetPasswordDto {
    @NotBlank(message = "Yangi parol kiritilishi shart")
    @Size(min = 6, message = "Yangi parol kamida 6 ta belgidan iborat bo'lishi kerak")
    private String newPassword;
}

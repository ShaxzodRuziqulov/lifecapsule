package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDto {
    @NotBlank(message = "Foydalanuvchi nomi bo'sh bo'lishi mumkin emas")
    @Size(min = 3, max = 50, message = "Foydalanuvchi nomi 3-50 belgidan iborat bo'lishi kerak")
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Foydalanuvchi nomida faqat lotin harflari, raqamlar, nuqta, _ va - bo'lishi mumkin")
    private String username;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String middleName;
}

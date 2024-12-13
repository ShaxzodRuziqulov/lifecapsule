/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:11.12.2024
 * TIME:11:58
 */
package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public class LoginDto {
    @Email(message = "Email noto'g'ri formatda")
    private String email;

    @NotBlank(message = "Parol bo'sh bo'lishi mumkin emas")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

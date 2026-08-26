/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:11.12.2024
 * TIME:11:59
 */
package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterUserDto {
    @NotBlank(message = "Foydalanuvchi ismi bo'sh bo'lishi mumkin emas")
    private String username;

    @NotBlank(message = "Email bo'sh bo'lishi mumkin emas")
    @Email(message = "Email noto'g'ri formatda")
    private String email;

    @NotBlank(message = "Ism bo'sh bo'lishi mumkin emas")
    @Size(max = 100, message = "Ism 100 belgidan oshmasligi kerak")
    private String firstName;

    @NotBlank(message = "Familiya bo'sh bo'lishi mumkin emas")
    @Size(max = 100, message = "Familiya 100 belgidan oshmasligi kerak")
    private String lastName;

    @Size(max = 100, message = "Otasining ismi 100 belgidan oshmasligi kerak")
    private String middleName;

    @NotBlank(message = "Parol bo'sh bo'lishi mumkin emas")
    @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateFamilyAccessDto {
    private Long userId;

    @Email(message = "Email manzil noto'g'ri")
    private String email;

    private FamilyAccessRole accessRole = FamilyAccessRole.VIEWER;
}

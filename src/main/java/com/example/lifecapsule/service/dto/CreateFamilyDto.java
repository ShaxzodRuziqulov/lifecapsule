package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.FamilyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateFamilyDto {
    @NotBlank(message = "Oila nomi bo'sh bo'lishi mumkin emas")
    @Size(max = 150, message = "Oila nomi 150 belgidan oshmasligi kerak")
    private String name;

    @Size(max = 2000, message = "Tavsif 2000 belgidan oshmasligi kerak")
    private String description;

    @NotNull(message = "Oila ko'rinishi tanlanishi kerak")
    private FamilyVisibility visibility = FamilyVisibility.INVITE_ONLY;
}

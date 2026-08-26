package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateFamilyAccessDto {
    @NotNull(message = "Ruxsat roli tanlanishi kerak")
    private FamilyAccessRole accessRole;

    @NotNull(message = "Ruxsat holati tanlanishi kerak")
    private AccessStatus status;
}

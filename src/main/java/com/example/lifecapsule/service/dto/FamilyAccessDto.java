package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FamilyAccessDto {
    private Long id;
    private Long familyId;
    private String familyName;
    private Long userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private FamilyAccessRole accessRole;
    private AccessStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}

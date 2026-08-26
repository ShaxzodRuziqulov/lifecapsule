package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.FamilyVisibility;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FamilyDto {
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private FamilyVisibility visibility;
    private FamilyAccessRole accessRole;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}

package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.FamilyVisibility;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminFamilySummaryDto {
    private Long id;
    private String name;
    private String description;
    private FamilyVisibility visibility;
    private String ownerUsername;
    private String ownerEmail;
    private long memberCount;
    private LocalDateTime createdAt;
}

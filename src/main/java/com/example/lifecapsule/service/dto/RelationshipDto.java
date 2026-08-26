package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.RelationshipType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RelationshipDto {
    private Long id;
    private Long familyId;
    private Long fromPersonId;
    private Long toPersonId;
    private RelationshipType type;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}

package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.RelationshipType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateRelationshipDto {
    @NotNull(message = "Birinchi shaxs tanlanishi kerak")
    private Long fromPersonId;

    @NotNull(message = "Ikkinchi shaxs tanlanishi kerak")
    private Long toPersonId;

    @NotNull(message = "Qarindoshlik turi tanlanishi kerak")
    private RelationshipType type;

    @Size(max = 2000, message = "Izoh 2000 belgidan oshmasligi kerak")
    private String note;
}

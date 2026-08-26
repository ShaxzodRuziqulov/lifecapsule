package com.example.lifecapsule.service.mapper;

import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.service.dto.CreateRelationshipDto;
import com.example.lifecapsule.service.dto.RelationshipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RelationshipMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "family", ignore = true)
    @Mapping(target = "fromPerson", ignore = true)
    @Mapping(target = "toPerson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    Relationship toEntity(CreateRelationshipDto dto);

    @Mapping(target = "familyId", source = "family.id")
    @Mapping(target = "fromPersonId", source = "fromPerson.id")
    @Mapping(target = "toPersonId", source = "toPerson.id")
    RelationshipDto toDto(Relationship relationship);
}

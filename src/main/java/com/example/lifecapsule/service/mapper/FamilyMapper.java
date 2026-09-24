package com.example.lifecapsule.service.mapper;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.service.dto.CreateFamilyDto;
import com.example.lifecapsule.service.dto.FamilyDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FamilyMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "coverStoredFileName", ignore = true)
    @Mapping(target = "coverOriginalFileName", ignore = true)
    @Mapping(target = "coverContentType", ignore = true)
    Family toEntity(CreateFamilyDto dto);

    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "accessRole", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    FamilyDto toDto(Family family);
}

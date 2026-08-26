package com.example.lifecapsule.service.mapper;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.service.dto.CreateFamilyAccessDto;
import com.example.lifecapsule.service.dto.FamilyAccessDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FamilyAccessMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "family", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    FamilyAccess toEntity(CreateFamilyAccessDto dto);

    @Mapping(target = "familyId", source = "family.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFirstName", source = "user.firstName")
    @Mapping(target = "userLastName", source = "user.lastName")
    FamilyAccessDto toDto(FamilyAccess access);
}

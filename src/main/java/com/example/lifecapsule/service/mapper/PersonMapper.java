package com.example.lifecapsule.service.mapper;

import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.service.dto.CreatePersonDto;
import com.example.lifecapsule.service.dto.PersonDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "family", ignore = true)
    @Mapping(target = "linkedUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "photoUrl", ignore = true)
    @Mapping(target = "videoUrl", ignore = true)
    @Mapping(target = "avatarStoredFileName", ignore = true)
    @Mapping(target = "avatarOriginalFileName", ignore = true)
    @Mapping(target = "avatarContentType", ignore = true)
    Person toEntity(CreatePersonDto dto);

    @Mapping(target = "familyId", source = "family.id")
    @Mapping(target = "linkedUserId", source = "linkedUser.id")
    @Mapping(target = "avatarUrl", ignore = true)
    PersonDto toDto(Person person);
}

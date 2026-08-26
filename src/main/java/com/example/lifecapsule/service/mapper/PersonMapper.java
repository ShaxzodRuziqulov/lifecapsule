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
    Person toEntity(CreatePersonDto dto);

    @Mapping(target = "familyId", source = "family.id")
    @Mapping(target = "linkedUserId", source = "linkedUser.id")
    PersonDto toDto(Person person);
}

package com.example.lifecapsule.service.mapper;

import com.example.lifecapsule.entity.Media;
import com.example.lifecapsule.service.dto.MediaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    @Mapping(target = "familyId", source = "family.id")
    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "taggedPersonIds", ignore = true)
    MediaDto toDto(Media media);
}

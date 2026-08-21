/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:20:02
 */
package com.example.lifecapsule.service.mapper;


import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.dto.RegisterUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "username", source = "userName")
    UserDto toDto(Users user);

    @Mapping(target = "userName", source = "username")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    Users toEntity(UserDto userDto);

    @Mapping(target = "userName", source = "username")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    Users toUser(RegisterUserDto registerUserDto);
}

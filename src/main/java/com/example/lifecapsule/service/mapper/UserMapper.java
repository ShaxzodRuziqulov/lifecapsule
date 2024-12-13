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
    @Mapping(target = "", source = "")
    UserDto toDto(Users user);

    Users toEntity(UserDto userDto);

    @Mapping(source = "", target = "")
    Users toUser(RegisterUserDto registerUserDto);
}

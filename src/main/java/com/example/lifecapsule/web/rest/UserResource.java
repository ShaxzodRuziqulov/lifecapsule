/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:19:54
 */
package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.mapper.UserMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class UserResource {
    private final UserMapper userMapper;

    public UserResource(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public UserDto currentUser(@AuthenticationPrincipal Users user) {
        return userMapper.toDto(user);
    }
}

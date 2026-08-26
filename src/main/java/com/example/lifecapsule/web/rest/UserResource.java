/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:19:54
 */
package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.UserService;
import com.example.lifecapsule.service.dto.ChangePasswordDto;
import com.example.lifecapsule.service.dto.UpdateUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class UserResource {
    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDto currentUser(@AuthenticationPrincipal Users user) {
        return userService.getCurrentUser(user);
    }

    @PutMapping
    public UserDto updateProfile(
            @AuthenticationPrincipal Users user,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateUserDto input
    ) {
        return userService.updateProfile(user, input);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Users user,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChangePasswordDto input
    ) {
        userService.changePassword(user, input);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal Users user) {
        userService.deactivate(user);
        return ResponseEntity.noContent().build();
    }
}

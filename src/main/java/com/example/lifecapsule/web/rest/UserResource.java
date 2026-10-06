/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:19:54
 */
package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.JwtService;
import com.example.lifecapsule.service.UserService;
import com.example.lifecapsule.service.dto.ChangePasswordDto;
import com.example.lifecapsule.service.dto.UpdateUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.response.LoginResponse;
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
    private final JwtService jwtService;

    public UserResource(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<UserDto> currentUser(@AuthenticationPrincipal Users user) {
        UserDto result = userService.getCurrentUser(user);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal Users user,
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateUserDto input
    ) {
        UserDto result = userService.updateProfile(user, input);
        return ResponseEntity.ok().body(result);
    }

    /** Every earlier token stops working once the password changes, so the caller gets a fresh pair. */
    @PatchMapping("/password")
    public ResponseEntity<LoginResponse> changePassword(
            @AuthenticationPrincipal Users user,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChangePasswordDto input
    ) {
        userService.changePassword(user, input);
        return ResponseEntity.ok(new LoginResponse(
                jwtService.generateToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.getExpirationTime()
        ));
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal Users user) {
        userService.deactivate(user);
        return ResponseEntity.noContent().build();
    }
}

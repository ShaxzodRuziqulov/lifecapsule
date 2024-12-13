/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:11.12.2024
 * TIME:12:34
 */
package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.AuthenticationService;
import com.example.lifecapsule.service.JwtService;
import com.example.lifecapsule.service.dto.LoginDto;
import com.example.lifecapsule.service.dto.RegisterUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.response.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
public class AuthenticationResource {
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthenticationResource(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
        UserDto userDto = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginDto input) {
        Users authenticatedUser = authenticationService.authenticate(input);

        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, "Bearer", jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

}

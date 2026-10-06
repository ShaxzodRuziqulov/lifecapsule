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
import com.example.lifecapsule.service.dto.RefreshTokenDto;
import com.example.lifecapsule.service.dto.RegisterUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.response.LoginResponse;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
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
        return ResponseEntity.ok().body(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginDto input) {
        Users authenticatedUser = authenticationService.authenticate(input);

        String jwtToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, refreshToken, "Bearer", jwtService.getExpirationTime());
        return ResponseEntity.ok().body(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenDto input) {
        try {
            Users user = authenticationService.findById(jwtService.extractUserId(input.getRefreshToken()));
            if (!jwtService.isRefreshTokenValid(input.getRefreshToken(), user)) {
                throw new BadCredentialsException("Refresh token yaroqsiz");
            }

            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            LoginResponse loginResponse = new LoginResponse(jwtToken, refreshToken, "Bearer", jwtService.getExpirationTime());
            return ResponseEntity.ok().body(loginResponse);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Refresh token yaroqsiz");
        }
    }

}

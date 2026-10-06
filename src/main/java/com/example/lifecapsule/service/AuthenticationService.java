/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:11.12.2024
 * TIME:12:00
 */
package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.entity.enumirated.Status;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.LoginDto;
import com.example.lifecapsule.service.dto.RegisterUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthenticationService(UserMapper userMapper, UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }


    @Transactional
    public UserDto signup(RegisterUserDto input) {
        String email = input.getEmail().trim().toLowerCase();
        String username = input.getUsername().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Bu email allaqachon ro'yxatdan o'tgan");
        }
        if (userRepository.existsByUserNameIgnoreCase(username)) {
            throw new ConflictException("Bu foydalanuvchi nomi band");
        }

        Users user = userMapper.toUser(input);
        user.setEmail(email);
        user.setUserName(username);
        user.setFirstName(input.getFirstName().trim());
        user.setLastName(input.getLastName().trim());
        user.setMiddleName(input.getMiddleName() == null ? null : input.getMiddleName().trim());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setStatus(Status.ACTIVE);
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public Users authenticate(LoginDto input) {
        if (input.getUsername() == null || input.getUsername().isBlank()) {
            throw new IllegalArgumentException("Foydalanuvchi nomi kiritilishi shart");
        }
        if (input.getPassword() == null || input.getPassword().isEmpty()) {
            throw new IllegalArgumentException(("Parol kiritilishi shart"));
        }
        String username = input.getUsername().trim();
        loginAttemptService.checkAllowed(username);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, input.getPassword()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(username);
            throw new BadCredentialsException("Foydalanuvchi nomi yoki parol noto'g'ri");
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Bu akkaunt faol emas");
        }
        loginAttemptService.recordSuccess(username);
        return userRepository.findByUserNameIgnoreCase(username)
                .orElseThrow(() -> new BadCredentialsException("Foydalanuvchi nomi yoki parol noto'g'ri"));
    }

    public Users findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Refresh token yaroqsiz"));
    }

}


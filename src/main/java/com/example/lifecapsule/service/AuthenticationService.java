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
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.LoginDto;
import com.example.lifecapsule.service.dto.RegisterUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserMapper userMapper, UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }


    public UserDto signup(RegisterUserDto input) {
        Users user = userMapper.toUser(input);
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setStatus(Status.ACTIVE);
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public Users authenticate(LoginDto input) {
        if (input.getEmail() == null || input.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email kiritilishi shart");
        }
        if (input.getPassword() == null || input.getPassword().isEmpty()) {
            throw new IllegalArgumentException(("Parol kiritilishi shart"));
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            input.getEmail(),
                            input.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Email yoki paro noto'g'ri");
        }
        return userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Foydalanuvchi topilmadi " + input.getEmail()));
    }

}


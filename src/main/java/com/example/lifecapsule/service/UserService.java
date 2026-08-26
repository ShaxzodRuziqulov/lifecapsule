/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:19:53
 */
package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.Status;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.UpdateUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.dto.ChangePasswordDto;
import com.example.lifecapsule.service.mapper.UserMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto getCurrentUser(Users currentUser) {
        return userMapper.toDto(currentUser);
    }

    @Transactional
    public UserDto updateProfile(Users currentUser, UpdateUserDto input) {
        String username = input.getUsername().trim();
        if (!username.equalsIgnoreCase(currentUser.getUserName())
                && userRepository.existsByUserNameIgnoreCase(username)) {
            throw new IllegalArgumentException("Bu foydalanuvchi nomi band");
        }

        currentUser.setUserName(username);
        currentUser.setFirstName(input.getFirstName().trim());
        currentUser.setLastName(input.getLastName().trim());
        currentUser.setMiddleName(input.getMiddleName() == null ? null : input.getMiddleName().trim());
        return userMapper.toDto(userRepository.save(currentUser));
    }

    @Transactional
    public void changePassword(Users currentUser, ChangePasswordDto input) {
        String oldPassword = input.getOldPassword();
        String newPassword = input.getNewPassword();

        if (oldPassword == null || oldPassword.isBlank()) {
            throw new IllegalArgumentException("Eski parol kiritilishi shart");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new IllegalArgumentException("Yangi parol kamida 6 ta belgidan iborat bo'lishi kerak");
        }
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new BadCredentialsException("Eski parol noto'g'ri");
        }
        if (passwordEncoder.matches(newPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("Yangi parol eski paroldan farq qilishi kerak");
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }

    @Transactional
    public void deactivate(Users currentUser) {
        currentUser.setStatus(Status.INACTIVE);
        userRepository.save(currentUser);
    }
}

/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:19:53
 */
package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.Status;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.IdCount;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.AdminResetPasswordDto;
import com.example.lifecapsule.service.dto.AdminUserSummaryDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.UpdateUserDto;
import com.example.lifecapsule.service.dto.UserDto;
import com.example.lifecapsule.service.dto.UserSearchResultDto;
import com.example.lifecapsule.service.dto.ChangePasswordDto;
import com.example.lifecapsule.service.mapper.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Map<String, String> ADMIN_ALLOWED_SORTS = Map.of(
            "id", "id",
            "createdAt", "createdAt",
            "username", "userName"
    );

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final FamilyAccessRepository familyAccessRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, UserRepository userRepository, FamilyAccessRepository familyAccessRepository, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.familyAccessRepository = familyAccessRepository;
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
            throw new ConflictException("Bu foydalanuvchi nomi band");
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

        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            throw new BadCredentialsException("Eski parol noto'g'ri");
        }
        if (passwordEncoder.matches(newPassword, currentUser.getPassword())) {
            throw new IllegalArgumentException("Yangi parol eski paroldan farq qilishi kerak");
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }

    /**
     * Lets an ADMIN reset any user's forgotten password without knowing the old one -
     * the project has no email/SMS infrastructure for a self-service "forgot password" flow,
     * so the family's admin does this by hand instead.
     */
    @Transactional
    public void adminResetPassword(Users currentUser, String username, AdminResetPasswordDto input) {
        requireAdmin(currentUser);

        Users target = userRepository.findByUserNameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Bu nomdagi foydalanuvchi topilmadi"));

        target.setPassword(passwordEncoder.encode(input.getNewPassword()));
        userRepository.save(target);
    }

    /**
     * Backs the "invite by username" search box - deliberately requires at least
     * 2 characters and returns only a handful of matches so it can't be used to
     * enumerate every account in the system.
     */
    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchByUsername(Users currentUser, String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }
        return userRepository.findTop8ByUserNameContainingIgnoreCaseOrderByUserNameAsc(trimmed).stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> {
                    UserSearchResultDto dto = new UserSearchResultDto();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUserName());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryDto> getAllUsersForAdmin(Users currentUser, PageFilter filter) {
        requireAdmin(currentUser);
        Pageable pageable = filter.toPageable("createdAt", ADMIN_ALLOWED_SORTS);
        Page<Users> users = userRepository.findAll(pageable);
        Map<Long, Long> familyCounts = familyAccessRepository
                .countByUserIds(users.map(Users::getId).toList(), AccessStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
        Page<AdminUserSummaryDto> result = users.map(user -> {
            AdminUserSummaryDto dto = new AdminUserSummaryDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUserName());
            dto.setEmail(user.getEmail());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setRole(user.getRole());
            dto.setStatus(user.getStatus());
            dto.setFamilyCount(familyCounts.getOrDefault(user.getId(), 0L));
            dto.setCreatedAt(user.getCreatedAt());
            return dto;
        });
        return new PageResponse<>(result);
    }

    private void requireAdmin(Users currentUser) {
        if (!FamilyAuthorization.isAdmin(currentUser)) {
            throw new ForbiddenException("Bu amalni faqat administrator bajara oladi");
        }
    }

    @Transactional
    public void deactivate(Users currentUser) {
        currentUser.setStatus(Status.INACTIVE);
        userRepository.save(currentUser);
    }
}

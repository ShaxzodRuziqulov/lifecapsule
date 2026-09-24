package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.FamilyService;
import com.example.lifecapsule.service.UserService;
import com.example.lifecapsule.service.dto.AdminFamilySummaryDto;
import com.example.lifecapsule.service.dto.AdminResetPasswordDto;
import com.example.lifecapsule.service.dto.AdminUserSummaryDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminResource {
    private final UserService userService;
    private final FamilyService familyService;

    @PatchMapping("/users/{username}/password")
    public ResponseEntity<Void> resetPassword(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable String username,
            @Valid @RequestBody AdminResetPasswordDto input
    ) {
        userService.adminResetPassword(currentUser, username, input);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserSummaryDto>> listUsers(
            @AuthenticationPrincipal Users currentUser,
            @Valid @ModelAttribute PageFilter filter
    ) {
        return ResponseEntity.ok().body(userService.getAllUsersForAdmin(currentUser, filter));
    }

    @GetMapping("/families")
    public ResponseEntity<PageResponse<AdminFamilySummaryDto>> listFamilies(
            @AuthenticationPrincipal Users currentUser,
            @Valid @ModelAttribute PageFilter filter
    ) {
        return ResponseEntity.ok().body(familyService.getAllFamiliesForAdmin(currentUser, filter));
    }
}

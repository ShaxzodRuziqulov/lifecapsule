package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.FamilyAccessService;
import com.example.lifecapsule.service.dto.FamilyAccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/family-invitations")
@RequiredArgsConstructor
public class FamilyInvitationResource {
    private final FamilyAccessService familyAccessService;

    @GetMapping
    public ResponseEntity<List<FamilyAccessDto>> getMyInvitations(@AuthenticationPrincipal Users currentUser) {
        List<FamilyAccessDto> result = familyAccessService.getMyInvitations(currentUser);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/{accessId}/accept")
    public ResponseEntity<FamilyAccessDto> acceptInvitation(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long accessId
    ) {
        FamilyAccessDto result = familyAccessService.acceptInvitation(currentUser, accessId);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/{accessId}/reject")
    public ResponseEntity<FamilyAccessDto> rejectInvitation(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long accessId
    ) {
        FamilyAccessDto result = familyAccessService.rejectInvitation(currentUser, accessId);
        return ResponseEntity.ok().body(result);
    }
}

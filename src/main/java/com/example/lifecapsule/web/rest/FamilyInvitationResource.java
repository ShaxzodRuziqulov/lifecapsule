package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.FamilyAccessService;
import com.example.lifecapsule.service.dto.FamilyAccessDto;
import lombok.RequiredArgsConstructor;
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
    public List<FamilyAccessDto> getMyInvitations(@AuthenticationPrincipal Users currentUser) {
        return familyAccessService.getMyInvitations(currentUser);
    }

    @PostMapping("/{accessId}/accept")
    public FamilyAccessDto acceptInvitation(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long accessId
    ) {
        return familyAccessService.acceptInvitation(currentUser, accessId);
    }

    @PostMapping("/{accessId}/reject")
    public FamilyAccessDto rejectInvitation(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long accessId
    ) {
        return familyAccessService.rejectInvitation(currentUser, accessId);
    }
}

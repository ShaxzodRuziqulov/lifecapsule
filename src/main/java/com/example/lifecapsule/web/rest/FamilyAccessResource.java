package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.FamilyAccessService;
import com.example.lifecapsule.service.dto.CreateFamilyAccessDto;
import com.example.lifecapsule.service.dto.FamilyAccessDto;
import com.example.lifecapsule.service.dto.UpdateFamilyAccessDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/families/{familyId}/access")
@RequiredArgsConstructor
public class FamilyAccessResource {
    private final FamilyAccessService familyAccessService;

    @GetMapping
    public List<FamilyAccessDto> getFamilyAccesses(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId
    ) {
        return familyAccessService.getFamilyAccesses(currentUser, familyId);
    }

    @PostMapping
    public FamilyAccessDto addFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreateFamilyAccessDto input
    ) {
        return familyAccessService.addFamilyAccess(currentUser, familyId, input);
    }

    @PostMapping("/invitations")
    public FamilyAccessDto inviteFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreateFamilyAccessDto input
    ) {
        return familyAccessService.inviteFamilyAccess(currentUser, familyId, input);
    }

    @PutMapping("/{accessId}")
    public FamilyAccessDto updateFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long accessId,
            @Valid @RequestBody UpdateFamilyAccessDto input
    ) {
        return familyAccessService.updateFamilyAccess(currentUser, familyId, accessId, input);
    }

    @DeleteMapping("/{accessId}")
    public ResponseEntity<Void> removeFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long accessId
    ) {
        familyAccessService.removeFamilyAccess(currentUser, familyId, accessId);
        return ResponseEntity.noContent().build();
    }
}

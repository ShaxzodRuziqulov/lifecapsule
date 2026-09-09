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
    public ResponseEntity<List<FamilyAccessDto>> getFamilyAccesses(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId
    ) {
        List<FamilyAccessDto> result = familyAccessService.getFamilyAccesses(currentUser, familyId);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping
    public ResponseEntity<FamilyAccessDto> addFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreateFamilyAccessDto input
    ) {
        FamilyAccessDto result = familyAccessService.addFamilyAccess(currentUser, familyId, input);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/invitations")
    public ResponseEntity<FamilyAccessDto> inviteFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreateFamilyAccessDto input
    ) {
        FamilyAccessDto result = familyAccessService.inviteFamilyAccess(currentUser, familyId, input);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping("/{accessId}")
    public ResponseEntity<FamilyAccessDto> updateFamilyAccess(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long accessId,
            @Valid @RequestBody UpdateFamilyAccessDto input
    ) {
        FamilyAccessDto result = familyAccessService.updateFamilyAccess(currentUser, familyId, accessId, input);
        return ResponseEntity.ok().body(result);
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

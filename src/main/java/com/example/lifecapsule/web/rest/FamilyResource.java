package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.FamilyService;
import com.example.lifecapsule.service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
public class FamilyResource {
    private final FamilyService familyService;

    @PostMapping
    public ResponseEntity<FamilyDto> createFamily(
            @AuthenticationPrincipal Users currentUser,
            @Valid @RequestBody CreateFamilyDto input
    ) {
        FamilyDto result = familyService.createFamily(currentUser, input);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping
    public ResponseEntity<PageResponse<FamilyDto>> getMyFamilies(
            @AuthenticationPrincipal Users currentUser,
            @Valid @ModelAttribute PageFilter filter
    ) {
        PageResponse<FamilyDto> result = familyService.getMyFamilies(currentUser, filter);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{familyId}")
    public ResponseEntity<FamilyDto> getFamily(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId
    ) {
        FamilyDto result = familyService.getFamily(currentUser, familyId);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping("/{familyId}")
    public ResponseEntity<FamilyDto> updateFamily(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody UpdateFamilyDto input
    ) {
        FamilyDto result = familyService.updateFamily(currentUser, familyId, input);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/{familyId}")
    public ResponseEntity<Void> deleteFamily(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId
    ) {
        familyService.deleteFamily(currentUser, familyId);
        return ResponseEntity.noContent().build();
    }
}

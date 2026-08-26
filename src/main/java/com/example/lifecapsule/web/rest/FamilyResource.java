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
    public FamilyDto createFamily(
            @AuthenticationPrincipal Users currentUser,
            @Valid @RequestBody CreateFamilyDto input
    ) {
        return familyService.createFamily(currentUser, input);
    }

    @GetMapping
    public PageResponse<FamilyDto> getMyFamilies(
            @AuthenticationPrincipal Users currentUser,
            PageFilter filter
    ) {
        return familyService.getMyFamilies(currentUser, filter);
    }

    @GetMapping("/{familyId}")
    public FamilyDto getFamily(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId
    ) {
        return familyService.getFamily(currentUser, familyId);
    }

    @PutMapping("/{familyId}")
    public FamilyDto updateFamily(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody UpdateFamilyDto input
    ) {
        return familyService.updateFamily(currentUser, familyId, input);
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

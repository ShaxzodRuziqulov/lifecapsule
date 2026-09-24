package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.FamilyService;
import com.example.lifecapsule.service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

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

    @PostMapping("/{familyId}/cover")
    public ResponseEntity<FamilyDto> uploadCover(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(familyService.uploadCover(currentUser, familyId, file));
    }

    @GetMapping("/{familyId}/cover")
    public ResponseEntity<Resource> cover(@AuthenticationPrincipal Users currentUser, @PathVariable Long familyId) {
        FamilyService.CoverFile file = familyService.loadCover(currentUser, familyId);
        String encodedName = java.net.URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName).body(file.resource());
    }

    @DeleteMapping("/{familyId}/cover")
    public ResponseEntity<Void> deleteCover(@AuthenticationPrincipal Users currentUser, @PathVariable Long familyId) {
        familyService.deleteCover(currentUser, familyId);
        return ResponseEntity.noContent().build();
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

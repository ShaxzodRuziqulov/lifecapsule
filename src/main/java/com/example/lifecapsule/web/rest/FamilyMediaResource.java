package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.MediaService;
import com.example.lifecapsule.service.dto.MediaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Family-wide media browsing, as opposed to {@link MediaResource}'s per-person galleries -
 * everything the current user can see across the whole family in one list.
 */
@RestController
@RequestMapping("/families/{familyId}/media")
@RequiredArgsConstructor
public class FamilyMediaResource {
    private final MediaService mediaService;

    @GetMapping
    public ResponseEntity<List<MediaDto>> list(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId
    ) {
        List<MediaDto> result = mediaService.listForFamily(currentUser, familyId);
        return ResponseEntity.ok().body(result);
    }
}

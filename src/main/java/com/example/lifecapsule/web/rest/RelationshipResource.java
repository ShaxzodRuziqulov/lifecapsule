package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.RelationshipService;
import com.example.lifecapsule.service.dto.CreateRelationshipDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.RelationshipDto;
import com.example.lifecapsule.service.dto.UpdateRelationshipDto;
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

@RestController
@RequestMapping("/families/{familyId}/relationships")
@RequiredArgsConstructor
public class RelationshipResource {
    private final RelationshipService relationshipService;

    @PostMapping
    public RelationshipDto createRelationship(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreateRelationshipDto input
    ) {
        return relationshipService.createRelationship(currentUser, familyId, input);
    }

    @GetMapping
    public PageResponse<RelationshipDto> getRelationships(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            PageFilter filter
    ) {
        return relationshipService.getRelationships(currentUser, familyId, filter);
    }

    @GetMapping("/{relationshipId}")
    public RelationshipDto getRelationship(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long relationshipId
    ) {
        return relationshipService.getRelationship(currentUser, familyId, relationshipId);
    }

    @PutMapping("/{relationshipId}")
    public RelationshipDto updateRelationship(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long relationshipId,
            @Valid @RequestBody UpdateRelationshipDto input
    ) {
        return relationshipService.updateRelationship(currentUser, familyId, relationshipId, input);
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> deleteRelationship(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long relationshipId
    ) {
        relationshipService.deleteRelationship(currentUser, familyId, relationshipId);
        return ResponseEntity.noContent().build();
    }
}

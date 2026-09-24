package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.MediaService;
import com.example.lifecapsule.service.dto.MediaDto;
import com.example.lifecapsule.service.dto.UpdateMediaDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/families/{familyId}/persons/{personId}/media")
@RequiredArgsConstructor
public class MediaResource {
    private final MediaService mediaService;

    @PostMapping
    public ResponseEntity<MediaDto> upload(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption
    ) {
        MediaDto result = mediaService.upload(currentUser, familyId, personId, caption, file);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping
    public ResponseEntity<List<MediaDto>> list(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId
    ) {
        List<MediaDto> result = mediaService.list(currentUser, familyId, personId);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{mediaId}/file")
    public ResponseEntity<Resource> file(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @PathVariable Long mediaId
    ) {
        MediaService.MediaFile file = mediaService.loadFile(currentUser, familyId, personId, mediaId);
        String encodedName = java.net.URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .body(file.resource());
    }

    @PatchMapping("/{mediaId}")
    public ResponseEntity<MediaDto> updateCaption(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @PathVariable Long mediaId,
            @Valid @RequestBody UpdateMediaDto input
    ) {
        MediaDto result = mediaService.updateCaption(currentUser, familyId, personId, mediaId, input);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @PathVariable Long mediaId
    ) {
        mediaService.delete(currentUser, familyId, personId, mediaId);
        return ResponseEntity.noContent().build();
    }
}

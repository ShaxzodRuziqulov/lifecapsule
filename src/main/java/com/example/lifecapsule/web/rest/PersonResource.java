package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.PersonService;
import com.example.lifecapsule.service.dto.CreatePersonDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.PersonDto;
import com.example.lifecapsule.service.dto.PersonUpdateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/families/{familyId}/persons")
@RequiredArgsConstructor
public class PersonResource {
    private final PersonService personService;

    @PostMapping
    public ResponseEntity<PersonDto> createPerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreatePersonDto input
    ) {
        PersonDto result = personService.createPerson(currentUser, familyId, input);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PersonDto>> getPersons(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @ModelAttribute PageFilter filter
    ) {
        PageResponse<PersonDto> result = personService.getPersons(currentUser, familyId, filter);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{personId}")
    public ResponseEntity<PersonDto> getPerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId
    ) {
        PersonDto result = personService.getPerson(currentUser, familyId, personId);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping("/{personId}")
    public ResponseEntity<PersonDto> updatePerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @Valid @RequestBody PersonUpdateDto input
    ) {
        PersonDto result = personService.updatePerson(currentUser, familyId, personId, input);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/{personId}/avatar")
    public ResponseEntity<PersonDto> uploadAvatar(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(personService.uploadAvatar(currentUser, familyId, personId, file));
    }

    @GetMapping("/{personId}/avatar")
    public ResponseEntity<Resource> avatar(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId
    ) {
        PersonService.AvatarFile file = personService.loadAvatar(currentUser, familyId, personId);
        String encodedName = java.net.URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName).body(file.resource());
    }

    @DeleteMapping("/{personId}/avatar")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId
    ) {
        personService.deleteAvatar(currentUser, familyId, personId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{personId}")
    public ResponseEntity<Void> deletePerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId
    ) {
        personService.deletePerson(currentUser, familyId, personId);
        return ResponseEntity.noContent().build();
    }
}

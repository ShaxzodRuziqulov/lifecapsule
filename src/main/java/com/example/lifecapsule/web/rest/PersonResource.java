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

    @PostMapping("/search")
    public ResponseEntity<PageResponse<PersonDto>> searchPersons(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody PageFilter filter
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

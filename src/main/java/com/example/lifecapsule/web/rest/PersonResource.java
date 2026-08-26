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
@RequestMapping("/families/{familyId}/persons")
@RequiredArgsConstructor
public class PersonResource {
    private final PersonService personService;

    @PostMapping
    public PersonDto createPerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @Valid @RequestBody CreatePersonDto input
    ) {
        return personService.createPerson(currentUser, familyId, input);
    }

    @GetMapping
    public PageResponse<PersonDto> getPersons(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            PageFilter filter
    ) {
        return personService.getPersons(currentUser, familyId, filter);
    }

    @GetMapping("/{personId}")
    public PersonDto getPerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId
    ) {
        return personService.getPerson(currentUser, familyId, personId);
    }

    @PutMapping("/{personId}")
    public PersonDto updatePerson(
            @AuthenticationPrincipal Users currentUser,
            @PathVariable Long familyId,
            @PathVariable Long personId,
            @Valid @RequestBody PersonUpdateDto input
    ) {
        return personService.updatePerson(currentUser, familyId, personId, input);
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

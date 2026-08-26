package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.CreatePersonDto;
import com.example.lifecapsule.service.dto.PersonDto;
import com.example.lifecapsule.service.mapper.PersonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServicePermissionTest {
    @Mock
    private PersonRepository personRepository;
    @Mock
    private RelationshipRepository relationshipRepository;
    @Mock
    private FamilyAccessRepository familyAccessRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonMapper personMapper;
    @InjectMocks
    private PersonService personService;

    @Test
    void viewerCannotCreatePerson() {
        Users viewer = user(3L);
        Family family = family(10L);
        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(access(family, viewer, FamilyAccessRole.VIEWER)));

        assertThatThrownBy(() -> personService.createPerson(viewer, 10L, input()))
                .isInstanceOf(ForbiddenException.class);
        verify(personRepository, never()).save(any());
    }

    @Test
    void editorCanCreatePerson() {
        Users editor = user(2L);
        Family family = family(10L);
        Person person = new Person();
        PersonDto dto = new PersonDto();
        dto.setFirstName("Ali");

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(access(family, editor, FamilyAccessRole.EDITOR)));
        when(personMapper.toEntity(any(CreatePersonDto.class))).thenReturn(person);
        when(personRepository.save(person)).thenReturn(person);
        when(personMapper.toDto(person)).thenReturn(dto);

        PersonDto result = personService.createPerson(editor, 10L, input());

        assertThat(result.getFirstName()).isEqualTo("Ali");
        assertThat(person.getFamily()).isEqualTo(family);
    }

    private CreatePersonDto input() {
        CreatePersonDto input = new CreatePersonDto();
        input.setFirstName("Ali");
        return input;
    }

    private Users user(Long id) {
        Users user = new Users();
        user.setId(id);
        user.setEmail("user" + id + "@lifecapsule.uz");
        user.setUserName("user" + id);
        return user;
    }

    private Family family(Long id) {
        Family family = new Family();
        family.setId(id);
        family.setName("Demo");
        return family;
    }

    private FamilyAccess access(Family family, Users user, FamilyAccessRole role) {
        FamilyAccess access = new FamilyAccess();
        access.setFamily(family);
        access.setUser(user);
        access.setAccessRole(role);
        access.setStatus(AccessStatus.ACTIVE);
        return access;
    }
}

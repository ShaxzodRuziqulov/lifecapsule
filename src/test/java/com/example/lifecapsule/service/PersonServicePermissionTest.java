package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.CreatePersonDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PersonDto;
import com.example.lifecapsule.service.mapper.PersonMapper;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
    private FamilyRepository familyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonMapper personMapper;
    @InjectMocks
    private PersonService personService;

    @BeforeEach
    void wireAuthorization() {
        org.springframework.test.util.ReflectionTestUtils.setField(personService, "authorization", new FamilyAuthorization(familyAccessRepository, familyRepository));
    }

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

    @Test
    void adminCanCreatePersonWithoutFamilyAccess() {
        Users admin = user(99L);
        admin.setRole(Role.ADMIN);
        Family family = family(10L);
        Person person = new Person();
        PersonDto dto = new PersonDto();
        dto.setFirstName("Ali");

        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(personMapper.toEntity(any(CreatePersonDto.class))).thenReturn(person);
        when(personRepository.save(person)).thenReturn(person);
        when(personMapper.toDto(person)).thenReturn(dto);

        PersonDto result = personService.createPerson(admin, 10L, input());

        assertThat(result.getFirstName()).isEqualTo("Ali");
        assertThat(person.getFamily()).isEqualTo(family);
    }

    @Test
    void userWithoutFamilyAccessCannotReadPerson() {
        Users outsider = user(9L);
        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 9L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.getPerson(outsider, 10L, 30L))
                .isInstanceOf(NotFoundException.class);
        verify(personRepository, never()).findByIdAndFamilyId(any(), any());
    }

    @Test
    void viewerGetsFemaleProfileWithSensitiveFieldsMasked() {
        Users viewer = user(3L);
        Family family = family(10L);
        Person person = person(family, 30L, Gender.FEMALE);
        PersonDto dto = detailedDto(Gender.FEMALE);

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(access(family, viewer, FamilyAccessRole.VIEWER)));
        when(personRepository.findByIdAndFamilyId(30L, 10L)).thenReturn(Optional.of(person));
        when(personMapper.toDto(person)).thenReturn(dto);

        PersonDto result = personService.getPerson(viewer, 10L, 30L);

        assertThat(result.getFirstName()).isEqualTo("Malika");
        assertThat(result.getLastName()).isEqualTo("Roziqulova");
        assertThat(result.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(result.getMaidenName()).isNull();
        assertThat(result.getBirthDate()).isNull();
        assertThat(result.getDeathDate()).isNull();
        assertThat(result.getBirthPlace()).isNull();
        assertThat(result.getOccupation()).isNull();
        assertThat(result.getLinkedUserId()).isNull();
        assertThat(result.getBiography()).contains("yopiq");
    }

    @Test
    void viewerGetsMaleProfileWithoutMasking() {
        Users viewer = user(3L);
        Family family = family(10L);
        Person person = person(family, 30L, Gender.MALE);
        PersonDto dto = detailedDto(Gender.MALE);

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(access(family, viewer, FamilyAccessRole.VIEWER)));
        when(personRepository.findByIdAndFamilyId(30L, 10L)).thenReturn(Optional.of(person));
        when(personMapper.toDto(person)).thenReturn(dto);

        PersonDto result = personService.getPerson(viewer, 10L, 30L);

        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(2003, 8, 9));
        assertThat(result.getBirthPlace()).isEqualTo("Toshkent");
        assertThat(result.getOccupation()).isEqualTo("Dizayner");
        assertThat(result.getLinkedUserId()).isEqualTo(77L);
    }

    @Test
    void editorGetsFemaleProfileWithoutMasking() {
        Users editor = user(2L);
        Family family = family(10L);
        Person person = person(family, 30L, Gender.FEMALE);
        PersonDto dto = detailedDto(Gender.FEMALE);

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(access(family, editor, FamilyAccessRole.EDITOR)));
        when(personRepository.findByIdAndFamilyId(30L, 10L)).thenReturn(Optional.of(person));
        when(personMapper.toDto(person)).thenReturn(dto);

        PersonDto result = personService.getPerson(editor, 10L, 30L);

        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(2003, 8, 9));
        assertThat(result.getBirthPlace()).isEqualTo("Toshkent");
        assertThat(result.getOccupation()).isEqualTo("Dizayner");
        assertThat(result.getLinkedUserId()).isEqualTo(77L);
    }

    @Test
    void viewerGetsMahramFemaleProfileWithoutMasking() {
        Users viewer = user(3L);
        Family family = family(10L);
        Person anvar = person(family, 20L, Gender.MALE);
        Person malika = person(family, 30L, Gender.FEMALE);
        Person kamol = person(family, 40L, Gender.MALE);
        PersonDto dto = detailedDto(Gender.FEMALE);

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(access(family, viewer, FamilyAccessRole.VIEWER)));
        when(personRepository.findByFamilyIdAndLinkedUserId(10L, 3L)).thenReturn(Optional.of(kamol));
        when(relationshipRepository.findAllByFamilyIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(
                relationship(family, anvar, malika, RelationshipType.PARENT),
                relationship(family, anvar, kamol, RelationshipType.PARENT)
        ));
        when(personRepository.findByIdAndFamilyId(30L, 10L)).thenReturn(Optional.of(malika));
        when(personMapper.toDto(malika)).thenReturn(dto);

        PersonDto result = personService.getPerson(viewer, 10L, 30L);

        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(2003, 8, 9));
        assertThat(result.getBirthPlace()).isEqualTo("Toshkent");
        assertThat(result.getOccupation()).isEqualTo("Dizayner");
        assertThat(result.getLinkedUserId()).isEqualTo(77L);
    }

    @Test
    void maskedPersonIdsExcludeTheViewersMahramWomen() {
        Users viewer = user(3L);
        Family family = family(10L);
        Person anvar = person(family, 20L, Gender.MALE);
        Person malika = person(family, 30L, Gender.FEMALE);
        Person kamol = person(family, 40L, Gender.MALE);

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(access(family, viewer, FamilyAccessRole.VIEWER)));
        when(personRepository.findByFamilyIdAndLinkedUserId(10L, 3L)).thenReturn(Optional.of(kamol));
        when(relationshipRepository.findAllByFamilyIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(
                relationship(family, anvar, malika, RelationshipType.PARENT),
                relationship(family, anvar, kamol, RelationshipType.PARENT)
        ));
        when(personRepository.findIdsByFamilyIdAndGender(10L, Gender.FEMALE)).thenReturn(List.of(30L, 50L));

        assertThat(personService.maskedPersonIds(viewer, 10L)).containsExactly(50L);
    }

    @Test
    void editorsSeeEveryProfile() {
        Users editor = user(2L);
        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(access(family(10L), editor, FamilyAccessRole.EDITOR)));

        assertThat(personService.maskedPersonIds(editor, 10L)).isEmpty();
        verify(personRepository, never()).findIdsByFamilyIdAndGender(any(), any());
    }

    @Test
    void viewerSearchUsesBasicPersonFieldsOnly() {
        Users viewer = user(3L);
        Family family = family(10L);
        PageFilter filter = new PageFilter();
        filter.setQ("Toshkent");

        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(access(family, viewer, FamilyAccessRole.VIEWER)));
        when(personRepository.searchBasicByFamilyId(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        personService.getPersons(viewer, 10L, filter);

        verify(personRepository).searchBasicByFamilyId(any(), any(), any());
        verify(personRepository, never()).searchByFamilyId(any(), any(), any());
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
        family.setName("Test family");
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

    private Person person(Family family, Long id, Gender gender) {
        Person person = new Person();
        person.setId(id);
        person.setFamily(family);
        person.setGender(gender);
        return person;
    }

    private PersonDto detailedDto(Gender gender) {
        PersonDto dto = new PersonDto();
        dto.setId(30L);
        dto.setFamilyId(10L);
        dto.setFirstName(gender == Gender.FEMALE ? "Malika" : "Shaxzod");
        dto.setLastName("Roziqulova");
        dto.setMaidenName("Karimova");
        dto.setGender(gender);
        dto.setBirthDate(LocalDate.of(2003, 8, 9));
        dto.setDeathDate(LocalDate.of(2090, 1, 1));
        dto.setBirthPlace("Toshkent");
        dto.setOccupation("Dizayner");
        dto.setBiography("Shaxsiy batafsil ma'lumot.");
        dto.setLinkedUserId(77L);
        return dto;
    }

    private Relationship relationship(Family family, Person from, Person to, RelationshipType type) {
        Relationship relationship = new Relationship();
        relationship.setFamily(family);
        relationship.setFromPerson(from);
        relationship.setToPerson(to);
        relationship.setType(type);
        return relationship;
    }
}

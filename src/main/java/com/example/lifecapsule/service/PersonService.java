package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.CreatePersonDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.PersonDto;
import com.example.lifecapsule.service.dto.PersonUpdateDto;
import com.example.lifecapsule.service.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonService {
    private static final String DEFAULT_SORT = "firstName";
    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "firstName", "firstName",
            "lastName", "lastName",
            "birthDate", "birthDate",
            "createdAt", "createdAt"
    );

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final FamilyAccessRepository familyAccessRepository;
    private final UserRepository userRepository;
    private final PersonMapper personMapper;

    @Transactional
    public PersonDto createPerson(Users currentUser, Long familyId, CreatePersonDto input) {
        FamilyAccess access = getEditableAccess(currentUser, familyId);

        if (input.getBirthDate() != null
                && input.getDeathDate() != null
                && input.getDeathDate().isBefore(input.getBirthDate())) {
            throw new IllegalArgumentException("Vafot etgan sana tug'ilgan sanadan oldin bo'lishi mumkin emas");
        }

        Person person = personMapper.toEntity(input);
        person.setFamily(access.getFamily());

        if (input.getLinkedUserId() != null) {
            Users linkedUser = userRepository.findById(input.getLinkedUserId())
                    .orElseThrow(() -> new NotFoundException("linkedUserId topilmadi"));
            person.setLinkedUser(linkedUser);
        }

        return personMapper.toDto(personRepository.save(person));
    }

    @Transactional
    public PersonDto updatePerson(Users currentUser, Long familyId, Long personId, PersonUpdateDto personDto) {
        if (personId == null) {
            throw new IllegalArgumentException("personId cannot be null");
        }

        getEditableAccess(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);

        if (personDto.getBirthDate() != null
                && personDto.getDeathDate() != null
                && personDto.getDeathDate().isBefore(personDto.getBirthDate())) {
            throw new IllegalArgumentException("Vafot etgan sana tug'ilgan sanadan oldin bo'lishi mumkin emas");
        }

        person.setFirstName(personDto.getFirstName().trim());
        person.setLastName(trimToNull(personDto.getLastName()));
        person.setMaidenName(trimToNull(personDto.getMaidenName()));
        person.setGender(personDto.getGender());
        person.setBirthDate(personDto.getBirthDate());
        person.setDeathDate(personDto.getDeathDate());
        person.setBirthPlace(trimToNull(personDto.getBirthPlace()));
        person.setOccupation(trimToNull(personDto.getOccupation()));
        person.setBiography(trimToNull(personDto.getBiography()));
        person.setPhotoUrl(trimToNull(personDto.getPhotoUrl()));

        Users linkedUser = null;
        if (personDto.getLinkedUserId() != null) {
            linkedUser = userRepository.findById(personDto.getLinkedUserId())
                    .orElseThrow(() -> new NotFoundException("linkedUserId topilmadi"));
        }
        person.setLinkedUser(linkedUser);

        return personMapper.toDto(personRepository.save(person));
    }

    @Transactional(readOnly = true)
    public PageResponse<PersonDto> getPersons(
            Users currentUser,
            Long familyId,
            PageFilter filter
    ) {
        FamilyAccess access = getReadableAccess(currentUser, familyId);
        PersonPrivacyContext privacyContext = createPrivacyContext(currentUser, access);
        String normalizedSort = PageableUtils.normalizeSort(filter.getSortBy(), DEFAULT_SORT);
        String normalizedDirection = PageableUtils.normalizeDirection(filter.getDirection());
        Pageable pageable = PageableUtils.create(
                filter.getPage(),
                filter.getSize(),
                normalizedSort,
                normalizedDirection,
                DEFAULT_SORT,
                ALLOWED_SORTS
        );

        String search = normalizeSearch(filter.getQ());
        var page = search == null
                ? personRepository.findAllByFamilyId(familyId, pageable)
                : searchByFamilyId(familyId, search, pageable, access);

        return PageResponse.from(page.map(person -> toDto(person, privacyContext)), normalizedSort, normalizedDirection);
    }

    @Transactional(readOnly = true)
    public PersonDto getPerson(Users currentUser, Long familyId, Long personId) {
        FamilyAccess access = getReadableAccess(currentUser, familyId);
        PersonPrivacyContext privacyContext = createPrivacyContext(currentUser, access);
        return toDto(getPersonEntity(familyId, personId), privacyContext);
    }

    @Transactional
    public void deletePerson(Users currentUser, Long familyId, Long personId) {
        getEditableAccess(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);
        relationshipRepository.deleteAllByFamilyIdAndPersonId(familyId, personId);
        personRepository.delete(person);
    }

    private Person getPersonEntity(Long familyId, Long personId) {
        return personRepository.findByIdAndFamilyId(personId, familyId)
                .orElseThrow(() -> new NotFoundException("Odam topilmadi"));
    }

    private PersonDto toDto(Person person, PersonPrivacyContext privacyContext) {
        PersonDto dto = personMapper.toDto(person);
        if (shouldMaskSensitiveProfile(person, privacyContext)) {
            maskSensitiveProfile(dto);
        }
        return dto;
    }

    private org.springframework.data.domain.Page<Person> searchByFamilyId(
            Long familyId,
            String search,
            Pageable pageable,
            FamilyAccess access
    ) {
        if (access.getAccessRole() == FamilyAccessRole.VIEWER) {
            return personRepository.searchBasicByFamilyId(familyId, search, pageable);
        }
        return personRepository.searchByFamilyId(familyId, search, pageable);
    }

    private PersonPrivacyContext createPrivacyContext(Users currentUser, FamilyAccess access) {
        if (access.getAccessRole() != FamilyAccessRole.VIEWER) {
            return new PersonPrivacyContext(access, Set.of());
        }

        Long familyId = access.getFamily().getId();
        Set<Long> visibleSensitivePersonIds = personRepository
                .findByFamilyIdAndLinkedUserId(familyId, currentUser.getId())
                .map(linkedPerson -> findMahramPersonIds(familyId, linkedPerson.getId()))
                .orElse(Set.of());

        return new PersonPrivacyContext(access, visibleSensitivePersonIds);
    }

    private Set<Long> findMahramPersonIds(Long familyId, Long linkedPersonId) {
        List<Relationship> relationships = relationshipRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId);
        Map<Long, Set<Long>> parentsByChild = new HashMap<>();
        Map<Long, Set<Long>> childrenByParent = new HashMap<>();
        Set<Long> visiblePersonIds = new HashSet<>();
        visiblePersonIds.add(linkedPersonId);

        for (Relationship relationship : relationships) {
            Long fromPersonId = relationship.getFromPerson().getId();
            Long toPersonId = relationship.getToPerson().getId();

            if (relationship.getType() == RelationshipType.PARTNER) {
                if (fromPersonId.equals(linkedPersonId)) {
                    visiblePersonIds.add(toPersonId);
                }
                if (toPersonId.equals(linkedPersonId)) {
                    visiblePersonIds.add(fromPersonId);
                }
                continue;
            }

            if (relationship.getType() == RelationshipType.PARENT
                    || relationship.getType() == RelationshipType.ADOPTIVE_PARENT) {
                parentsByChild.computeIfAbsent(toPersonId, ignored -> new HashSet<>()).add(fromPersonId);
                childrenByParent.computeIfAbsent(fromPersonId, ignored -> new HashSet<>()).add(toPersonId);
            }
        }

        visiblePersonIds.addAll(collectConnected(linkedPersonId, parentsByChild));
        visiblePersonIds.addAll(collectConnected(linkedPersonId, childrenByParent));

        Set<Long> parentIds = parentsByChild.getOrDefault(linkedPersonId, Set.of());
        for (Long parentId : parentIds) {
            visiblePersonIds.addAll(childrenByParent.getOrDefault(parentId, Set.of()));
        }

        return visiblePersonIds;
    }

    private Set<Long> collectConnected(Long startPersonId, Map<Long, Set<Long>> graph) {
        Set<Long> visited = new HashSet<>();
        Set<Long> next = new HashSet<>(graph.getOrDefault(startPersonId, Set.of()));

        while (!next.isEmpty()) {
            Long personId = next.iterator().next();
            next.remove(personId);
            if (visited.add(personId)) {
                next.addAll(graph.getOrDefault(personId, Set.of()));
            }
        }

        return visited;
    }

    private boolean shouldMaskSensitiveProfile(Person person, PersonPrivacyContext privacyContext) {
        if (privacyContext.access().getAccessRole() != FamilyAccessRole.VIEWER || person.getGender() != Gender.FEMALE) {
            return false;
        }
        return !privacyContext.visibleSensitivePersonIds().contains(person.getId());
    }

    private void maskSensitiveProfile(PersonDto dto) {
        dto.setMaidenName(null);
        dto.setBirthDate(null);
        dto.setDeathDate(null);
        dto.setBirthPlace(null);
        dto.setOccupation(null);
        dto.setBiography("Ko'ruvchi rolida bu profilning batafsil ma'lumotlari yopiq.");
        dto.setPhotoUrl(null);
        dto.setLinkedUserId(null);
    }

    private FamilyAccess getReadableAccess(Users currentUser, Long familyId) {
        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(familyId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q"));

        if (access.getStatus() != AccessStatus.ACTIVE) {
            throw new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q");
        }
        return access;
    }

    private FamilyAccess getEditableAccess(Users currentUser, Long familyId) {
        FamilyAccess access = getReadableAccess(currentUser, familyId);

        if (access.getStatus() != AccessStatus.ACTIVE || access.getAccessRole() == FamilyAccessRole.VIEWER) {
            throw new ForbiddenException("Sizda bu oilaga odam qo'shish huquqi yo'q");
        }
        return access;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSearch(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }

    private record PersonPrivacyContext(FamilyAccess access, Set<Long> visibleSensitivePersonIds) {
    }

}

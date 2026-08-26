package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
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

import java.util.Map;

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
        getReadableAccess(currentUser, familyId);
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
                : personRepository.searchByFamilyId(familyId, search, pageable);

        return PageResponse.from(page.map(personMapper::toDto), normalizedSort, normalizedDirection);
    }

    @Transactional(readOnly = true)
    public PersonDto getPerson(Users currentUser, Long familyId, Long personId) {
        getReadableAccess(currentUser, familyId);
        return personMapper.toDto(getPersonEntity(familyId, personId));
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

}

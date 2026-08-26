package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.service.dto.CreateRelationshipDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.RelationshipDto;
import com.example.lifecapsule.service.dto.UpdateRelationshipDto;
import com.example.lifecapsule.service.mapper.RelationshipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelationshipService {
    private static final String DEFAULT_SORT = "createdAt";
    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "createdAt", "createdAt",
            "type", "type",
            "fromFirstName", "fromPerson.firstName",
            "toFirstName", "toPerson.firstName"
    );

    private final RelationshipRepository relationshipRepository;
    private final PersonRepository personRepository;
    private final FamilyAccessRepository familyAccessRepository;
    private final RelationshipMapper relationshipMapper;

    @Transactional
    public RelationshipDto createRelationship(Users currentUser, Long familyId, CreateRelationshipDto input) {
        getEditableAccess(currentUser, familyId);
        validateDifferentPeople(input.getFromPersonId(), input.getToPersonId());
        validateDuplicate(familyId, input.getFromPersonId(), input.getToPersonId(), input.getType(), null);

        Relationship relationship = relationshipMapper.toEntity(input);
        relationship.setFamily(getReadableAccess(currentUser, familyId).getFamily());
        relationship.setFromPerson(getPersonEntity(familyId, input.getFromPersonId()));
        relationship.setToPerson(getPersonEntity(familyId, input.getToPersonId()));
        relationship.setNote(trimToNull(input.getNote()));

        return relationshipMapper.toDto(relationshipRepository.save(relationship));
    }

    @Transactional(readOnly = true)
    public PageResponse<RelationshipDto> getRelationships(
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
                ? relationshipRepository.findAllByFamilyId(familyId, pageable)
                : relationshipRepository.searchByFamilyId(familyId, search, pageable);

        return PageResponse.from(page.map(relationshipMapper::toDto), normalizedSort, normalizedDirection);
    }

    @Transactional(readOnly = true)
    public RelationshipDto getRelationship(Users currentUser, Long familyId, Long relationshipId) {
        getReadableAccess(currentUser, familyId);
        return relationshipMapper.toDto(getRelationshipEntity(familyId, relationshipId));
    }

    @Transactional
    public RelationshipDto updateRelationship(
            Users currentUser,
            Long familyId,
            Long relationshipId,
            UpdateRelationshipDto input
    ) {
        getEditableAccess(currentUser, familyId);
        validateDifferentPeople(input.getFromPersonId(), input.getToPersonId());
        validateDuplicate(familyId, input.getFromPersonId(), input.getToPersonId(), input.getType(), relationshipId);

        Relationship relationship = getRelationshipEntity(familyId, relationshipId);
        relationship.setFromPerson(getPersonEntity(familyId, input.getFromPersonId()));
        relationship.setToPerson(getPersonEntity(familyId, input.getToPersonId()));
        relationship.setType(input.getType());
        relationship.setNote(trimToNull(input.getNote()));

        return relationshipMapper.toDto(relationshipRepository.save(relationship));
    }

    @Transactional
    public void deleteRelationship(Users currentUser, Long familyId, Long relationshipId) {
        getEditableAccess(currentUser, familyId);
        relationshipRepository.delete(getRelationshipEntity(familyId, relationshipId));
    }

    private Relationship getRelationshipEntity(Long familyId, Long relationshipId) {
        return relationshipRepository.findByIdAndFamilyId(relationshipId, familyId)
                .orElseThrow(() -> new NotFoundException("Qarindoshlik topilmadi"));
    }

    private Person getPersonEntity(Long familyId, Long personId) {
        return personRepository.findByIdAndFamilyId(personId, familyId)
                .orElseThrow(() -> new NotFoundException("Odam topilmadi"));
    }

    private void validateDifferentPeople(Long fromPersonId, Long toPersonId) {
        if (fromPersonId.equals(toPersonId)) {
            throw new IllegalArgumentException("Bir odam o'zi bilan qarindosh qilib belgilanmaydi");
        }
    }

    private void validateDuplicate(
            Long familyId,
            Long fromPersonId,
            Long toPersonId,
            com.example.lifecapsule.entity.enumirated.RelationshipType type,
            Long ignoredRelationshipId
    ) {
        relationshipRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId)
                .stream()
                .filter(relationship -> ignoredRelationshipId == null || !relationship.getId().equals(ignoredRelationshipId))
                .filter(relationship -> relationship.getFromPerson().getId().equals(fromPersonId))
                .filter(relationship -> relationship.getToPerson().getId().equals(toPersonId))
                .filter(relationship -> relationship.getType() == type)
                .findFirst()
                .ifPresent(relationship -> {
                    throw new ConflictException("Bu qarindoshlik allaqachon mavjud");
                });
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

        if (access.getAccessRole() == FamilyAccessRole.VIEWER) {
            throw new ForbiddenException("Sizda bu oilani o'zgartirish huquqi yo'q");
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

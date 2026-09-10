package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.service.dto.CreateRelationshipDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.RelationshipDto;
import com.example.lifecapsule.service.dto.UpdateRelationshipDto;
import com.example.lifecapsule.service.mapper.RelationshipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import com.example.lifecapsule.entity.enumirated.RelationshipType;

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
    private final FamilyRepository familyRepository;
    private final RelationshipMapper relationshipMapper;

    @Transactional
    public RelationshipDto createRelationship(Users currentUser, Long familyId, CreateRelationshipDto input) {
        getEditableAccess(currentUser, familyId);
        lockFamily(familyId);
        validateDifferentPeople(input.getFromPersonId(), input.getToPersonId());
        validateRelationship(familyId, input.getFromPersonId(), input.getToPersonId(), input.getType(), null);

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
        Pageable pageable = filter.toPageable(DEFAULT_SORT, ALLOWED_SORTS);

        String search = filter.normalizedQuery();
        var page = relationshipRepository.searchByFamilyId(familyId, search, pageable);

        Page<RelationshipDto> result = page.map(relationshipMapper::toDto);
        return new PageResponse<>(result);
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
        lockFamily(familyId);
        Relationship relationship = getRelationshipEntity(familyId, relationshipId);
        validateDifferentPeople(input.getFromPersonId(), input.getToPersonId());
        validateRelationship(familyId, input.getFromPersonId(), input.getToPersonId(), input.getType(), relationshipId);

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

    private void lockFamily(Long familyId) {
        // Serialize graph additions/edits so concurrent requests cannot create a cycle.
        familyRepository.findByIdForUpdate(familyId)
                .orElseThrow(() -> new NotFoundException("Oila topilmadi"));
    }

    private void validateRelationship(
            Long familyId,
            Long fromPersonId,
            Long toPersonId,
            RelationshipType type,
            Long ignoredRelationshipId
    ) {
        var relationships = relationshipRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId)
                .stream()
                .filter(relationship -> ignoredRelationshipId == null || !relationship.getId().equals(ignoredRelationshipId))
                .toList();
        Map<Long, Set<Long>> children = new HashMap<>();
        for (Relationship relationship : relationships) {
            Long from = relationship.getFromPerson().getId();
            Long to = relationship.getToPerson().getId();
            boolean sameDirection = from.equals(fromPersonId) && to.equals(toPersonId);
            boolean reversePartner = type == RelationshipType.PARTNER
                    && from.equals(toPersonId) && to.equals(fromPersonId);
            if (relationship.getType() == type && (sameDirection || reversePartner)) {
                throw new ConflictException("Bu qarindoshlik allaqachon mavjud");
            }
            if (relationship.getType() == RelationshipType.PARENT
                    || relationship.getType() == RelationshipType.ADOPTIVE_PARENT) {
                children.computeIfAbsent(from, ignored -> new HashSet<>()).add(to);
            }
        }
        if (type == RelationshipType.PARTNER) return;

        // Adding parent -> child is invalid if child already reaches parent.
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(toPersonId);
        while (!pending.isEmpty()) {
            Long personId = pending.removeFirst();
            if (!visited.add(personId)) continue;
            if (personId.equals(fromPersonId)) {
                throw new ConflictException("Bu bog'lanishni qo'shib bo'lmaydi: avlodni o'z ajdodiga ota-ona qilib belgilayapsiz.");
            }
            pending.addAll(children.getOrDefault(personId, Set.of()));
        }
    }

    private FamilyAccess getReadableAccess(Users currentUser, Long familyId) {
        if (isAdmin(currentUser)) {
            return adminAccess(currentUser, familyId);
        }

        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(familyId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q"));

        if (access.getStatus() != AccessStatus.ACTIVE) {
            throw new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q");
        }
        return access;
    }

    private FamilyAccess adminAccess(Users currentUser, Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Oila topilmadi"));
        FamilyAccess access = new FamilyAccess();
        access.setFamily(family);
        access.setUser(currentUser);
        access.setAccessRole(FamilyAccessRole.OWNER);
        access.setStatus(AccessStatus.ACTIVE);
        return access;
    }

    private boolean isAdmin(Users currentUser) {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
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

}

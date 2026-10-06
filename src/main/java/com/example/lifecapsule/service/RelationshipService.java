package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.NotFoundException;
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

import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final FamilyAuthorization authorization;
    private final FamilyRepository familyRepository;
    private final RelationshipMapper relationshipMapper;

    @Transactional
    public RelationshipDto createRelationship(Users currentUser, Long familyId, CreateRelationshipDto input) {
        FamilyAccess access = authorization.editable(currentUser, familyId);
        lockFamily(familyId);
        validateDifferentPeople(input.getFromPersonId(), input.getToPersonId());
        validateRelationship(familyId, input.getFromPersonId(), input.getToPersonId(), input.getType(), null);

        Relationship relationship = relationshipMapper.toEntity(input);
        relationship.setFamily(access.getFamily());
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
        authorization.readable(currentUser, familyId);
        Pageable pageable = filter.toPageable(DEFAULT_SORT, ALLOWED_SORTS);

        String search = filter.normalizedQuery();
        var page = relationshipRepository.searchByFamilyId(familyId, search, pageable);

        Page<RelationshipDto> result = page.map(relationshipMapper::toDto);
        return new PageResponse<>(result);
    }

    @Transactional(readOnly = true)
    public RelationshipDto getRelationship(Users currentUser, Long familyId, Long relationshipId) {
        authorization.readable(currentUser, familyId);
        return relationshipMapper.toDto(getRelationshipEntity(familyId, relationshipId));
    }

    @Transactional
    public RelationshipDto updateRelationship(
            Users currentUser,
            Long familyId,
            Long relationshipId,
            UpdateRelationshipDto input
    ) {
        authorization.editable(currentUser, familyId);
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
        authorization.editable(currentUser, familyId);
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
        validateParentSlots(relationships, familyId, fromPersonId, toPersonId, type);
    }

    /**
     * A child has at most two parents of each kind (biological / adoptive), at most one of them a
     * man and one a woman, and the same person cannot be both kinds of parent to one child.
     */
    private void validateParentSlots(List<Relationship> relationships, Long familyId, Long parentId, Long childId, RelationshipType type) {
        boolean otherKind = relationships.stream().anyMatch(relationship -> relationship.getType() != type
                && relationship.getType() != RelationshipType.PARTNER
                && relationship.getFromPerson().getId().equals(parentId)
                && relationship.getToPerson().getId().equals(childId));
        if (otherKind) {
            throw new ConflictException("Bu odam shu farzandga allaqachon boshqa turdagi ota-ona sifatida bog'langan");
        }
        List<Person> existingParents = relationships.stream()
                .filter(relationship -> relationship.getType() == type && relationship.getToPerson().getId().equals(childId))
                .map(Relationship::getFromPerson)
                .toList();
        String child = getPersonEntity(familyId, childId).getFirstName();
        String kind = type == RelationshipType.ADOPTIVE_PARENT ? "asrab olgan " : "";
        if (existingParents.size() >= 2) {
            throw new ConflictException(child + "ning " + kind + "ota-onasi (ikkalasi ham) allaqachon kiritilgan");
        }
        Gender gender = getPersonEntity(familyId, parentId).getGender();
        if ((gender == Gender.MALE || gender == Gender.FEMALE)
                && existingParents.stream().anyMatch(parent -> parent.getGender() == gender)) {
            throw new ConflictException(child + "ning " + kind + (gender == Gender.MALE ? "otasi" : "onasi") + " allaqachon kiritilgan");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}

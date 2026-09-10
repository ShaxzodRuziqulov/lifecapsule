package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.service.dto.*;
import com.example.lifecapsule.service.mapper.FamilyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FamilyService {
    private static final String DEFAULT_SORT = "createdAt";
    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "id", "family.id",
            "createdAt", "family.createdAt",
            "name", "family.name",
            "visibility", "family.visibility"
    );
    private static final Map<String, String> ADMIN_ALLOWED_SORTS = Map.of(
            "id", "id",
            "createdAt", "createdAt",
            "name", "name",
            "visibility", "visibility"
    );

    private final FamilyRepository familyRepository;
    private final FamilyAccessRepository familyAccessRepository;
    private final FamilyMapper familyMapper;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;

    @Transactional
    public FamilyDto createFamily(Users currentUser, CreateFamilyDto input) {
        String name = input.getName().trim();
        String description = trimToNull(input.getDescription());

        Family family = familyMapper.toEntity(input);
        family.setName(name);
        family.setDescription(description);
        family.setCreatedBy(currentUser);

        Family savedFamily = familyRepository.save(family);

        FamilyAccess access = new FamilyAccess();
        access.setFamily(savedFamily);
        access.setUser(currentUser);
        access.setAccessRole(FamilyAccessRole.OWNER);
        access.setStatus(AccessStatus.ACTIVE);

        familyAccessRepository.save(access);
        return toDto(access);
    }

    @Transactional(readOnly = true)
    public PageResponse<FamilyDto> getMyFamilies(Users currentUser, PageFilter filter) {
        String search = filter.normalizedQuery();

        if (isAdmin(currentUser)) {
            Pageable pageable = filter.toPageable(DEFAULT_SORT, ADMIN_ALLOWED_SORTS);
            Page<FamilyDto> result = familyRepository
                    .searchAllFamiliesPaging(search, pageable)
                    .map(family -> toDto(family, FamilyAccessRole.OWNER));
            return new PageResponse<>(result);
        }

        Pageable pageable = filter.toPageable(DEFAULT_SORT, ALLOWED_SORTS);
        Page<FamilyDto> result = familyAccessRepository
                .searchMyFamiliesPaging(currentUser.getId(), AccessStatus.ACTIVE, search, pageable)
                .map(this::toDto);
        return new PageResponse<>(result);
    }

    @Transactional(readOnly = true)
    public FamilyDto getFamily(Users currentUser, Long familyId) {
        if (isAdmin(currentUser)) {
            return toDto(getFamilyEntity(familyId), FamilyAccessRole.OWNER);
        }
        return toDto(getActiveAccess(currentUser, familyId));
    }

    @Transactional
    public FamilyDto updateFamily(Users currentUser, Long familyId, UpdateFamilyDto input) {
        FamilyAccess access = isAdmin(currentUser) ? null : getActiveAccess(currentUser, familyId);

        if (access != null && access.getAccessRole() == FamilyAccessRole.VIEWER) {
            throw new ForbiddenException("Sizda oilani o'zgartirish huquqi yo'q");
        }

        Family family = access == null ? getFamilyEntity(familyId) : access.getFamily();
        family.setName(input.getName().trim());
        family.setDescription(trimToNull(input.getDescription()));
        family.setVisibility(input.getVisibility());

        familyRepository.save(family);
        return access == null ? toDto(family, FamilyAccessRole.OWNER) : toDto(access);
    }

    @Transactional
    public void deleteFamily(Users currentUser, Long familyId) {
        FamilyAccess access = isAdmin(currentUser) ? null : getActiveAccess(currentUser, familyId);

        if (access != null && access.getAccessRole() != FamilyAccessRole.OWNER) {
            throw new ForbiddenException("Oilani faqat egasi o'chira oladi");
        }

        relationshipRepository.deleteAllByFamilyId(familyId);
        personRepository.deleteAllByFamilyId(familyId);
        familyAccessRepository.deleteAllByFamilyId(familyId);
        familyRepository.delete(access == null ? getFamilyEntity(familyId) : access.getFamily());
    }

    private FamilyDto toDto(FamilyAccess access) {
        return toDto(access.getFamily(), access.getAccessRole());
    }

    private FamilyDto toDto(Family family, FamilyAccessRole accessRole) {
        FamilyDto dto = familyMapper.toDto(family);
        dto.setAccessRole(accessRole);
        return dto;
    }

    private FamilyAccess getActiveAccess(Users currentUser, Long familyId) {
        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(familyId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q"));

        if (access.getStatus() != AccessStatus.ACTIVE) {
            throw new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q");
        }
        return access;
    }

    private Family getFamilyEntity(Long familyId) {
        return familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Oila topilmadi"));
    }

    private boolean isAdmin(Users currentUser) {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}

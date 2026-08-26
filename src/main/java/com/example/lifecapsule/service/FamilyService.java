package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.service.dto.CreateFamilyDto;
import com.example.lifecapsule.service.dto.FamilyDto;
import com.example.lifecapsule.service.dto.PageFilter;
import com.example.lifecapsule.service.dto.PageResponse;
import com.example.lifecapsule.service.dto.UpdateFamilyDto;
import com.example.lifecapsule.service.mapper.FamilyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FamilyService {
    private static final String DEFAULT_SORT = "createdAt";
    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "createdAt", "createdAt",
            "name", "family.name",
            "visibility", "family.visibility"
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
                ? familyAccessRepository.findAllByUserIdAndStatus(currentUser.getId(), AccessStatus.ACTIVE, pageable)
                : familyAccessRepository.searchMyFamilies(currentUser.getId(), AccessStatus.ACTIVE, search, pageable);

        return PageResponse.from(page.map(this::toDto), normalizedSort, normalizedDirection);
    }

    @Transactional(readOnly = true)
    public FamilyDto getFamily(Users currentUser, Long familyId) {
        return toDto(getActiveAccess(currentUser, familyId));
    }

    @Transactional
    public FamilyDto updateFamily(Users currentUser, Long familyId, UpdateFamilyDto input) {
        FamilyAccess access = getActiveAccess(currentUser, familyId);

        if (access.getAccessRole() == FamilyAccessRole.VIEWER) {
            throw new ForbiddenException("Sizda oilani o'zgartirish huquqi yo'q");
        }

        Family family = access.getFamily();
        family.setName(input.getName().trim());
        family.setDescription(trimToNull(input.getDescription()));
        family.setVisibility(input.getVisibility());

        familyRepository.save(family);
        return toDto(access);
    }

    @Transactional
    public void deleteFamily(Users currentUser, Long familyId) {
        FamilyAccess access = getActiveAccess(currentUser, familyId);

        if (access.getAccessRole() != FamilyAccessRole.OWNER) {
            throw new ForbiddenException("Oilani faqat egasi o'chira oladi");
        }

        relationshipRepository.deleteAllByFamilyId(familyId);
        personRepository.deleteAllByFamilyId(familyId);
        familyAccessRepository.deleteAllByFamilyId(familyId);
        familyRepository.delete(access.getFamily());
    }

    private Family getFamilyIfUserHasAccess(Users currentUser, Long familyId) {
        return getActiveAccess(currentUser, familyId).getFamily();
    }

    private FamilyDto toDto(FamilyAccess access) {
        FamilyDto dto = familyMapper.toDto(access.getFamily());
        dto.setAccessRole(access.getAccessRole());
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

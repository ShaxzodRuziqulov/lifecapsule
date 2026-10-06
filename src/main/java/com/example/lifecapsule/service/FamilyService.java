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
import com.example.lifecapsule.repository.IdCount;
import com.example.lifecapsule.repository.MediaRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.service.dto.*;
import com.example.lifecapsule.service.mapper.FamilyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyService {
    private static final long MAX_COVER_FILE_SIZE = 15L * 1024 * 1024;
    private static final Set<String> ALLOWED_COVER_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
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
    private final FamilyAuthorization authorization;
    private final FamilyMapper familyMapper;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final MediaRepository mediaRepository;
    private final StorageService storageService;

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

    /** Membership-only even for ADMIN; the system-wide view is getAllFamiliesForAdmin. */
    @Transactional(readOnly = true)
    public PageResponse<FamilyDto> getMyFamilies(Users currentUser, PageFilter filter) {
        String search = filter.normalizedQuery();
        Pageable pageable = filter.toPageable(DEFAULT_SORT, ALLOWED_SORTS);
        Page<FamilyDto> result = familyAccessRepository
                .searchMyFamiliesPaging(currentUser.getId(), AccessStatus.ACTIVE, search, pageable)
                .map(this::toDto);
        return new PageResponse<>(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminFamilySummaryDto> getAllFamiliesForAdmin(Users currentUser, PageFilter filter) {
        if (!FamilyAuthorization.isAdmin(currentUser)) {
            throw new ForbiddenException("Bu amalni faqat administrator bajara oladi");
        }
        Pageable pageable = filter.toPageable(DEFAULT_SORT, ADMIN_ALLOWED_SORTS);
        Page<Family> families = familyRepository.searchAllFamiliesPaging(filter.normalizedQuery(), pageable);
        Map<Long, Long> memberCounts = familyAccessRepository
                .countByFamilyIds(families.map(Family::getId).toList(), AccessStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
        Page<AdminFamilySummaryDto> result = families
                .map(family -> {
                    AdminFamilySummaryDto dto = new AdminFamilySummaryDto();
                    dto.setId(family.getId());
                    dto.setName(family.getName());
                    dto.setDescription(family.getDescription());
                    dto.setVisibility(family.getVisibility());
                    dto.setOwnerUsername(family.getCreatedBy().getUserName());
                    dto.setOwnerEmail(family.getCreatedBy().getEmail());
                    dto.setMemberCount(memberCounts.getOrDefault(family.getId(), 0L));
                    dto.setCreatedAt(family.getCreatedAt());
                    return dto;
                });
        return new PageResponse<>(result);
    }

    @Transactional(readOnly = true)
    public FamilyDto getFamily(Users currentUser, Long familyId) {
        return toDto(authorization.readable(currentUser, familyId));
    }

    /** Name, description and especially visibility are owner decisions, matching the settings UI. */
    @Transactional
    public FamilyDto updateFamily(Users currentUser, Long familyId, UpdateFamilyDto input) {
        FamilyAccess access = authorization.owner(currentUser, familyId);
        Family family = access.getFamily();
        family.setName(input.getName().trim());
        family.setDescription(trimToNull(input.getDescription()));
        family.setVisibility(input.getVisibility());

        familyRepository.save(family);
        return toDto(access);
    }

    @Transactional
    public void deleteFamily(Users currentUser, Long familyId) {
        Family family = authorization.owner(currentUser, familyId).getFamily();
        var media = mediaRepository.findAllByFamilyId(familyId);
        String coverPath = family.getCoverStoredFileName();
        relationshipRepository.deleteAllByFamilyId(familyId);
        mediaRepository.clearTagsForFamily(familyId);
        personRepository.deleteAllByFamilyId(familyId);
        mediaRepository.deleteAllByFamilyId(familyId);
        familyAccessRepository.deleteAllByFamilyId(familyId);
        familyRepository.delete(family);
        media.forEach(item -> storageService.delete(item.getStoredFileName()));
        if (coverPath != null) storageService.delete(coverPath);
    }

    @Transactional
    public FamilyDto uploadCover(Users currentUser, Long familyId, MultipartFile file) {
        FamilyAccess access = authorization.owner(currentUser, familyId);
        Family family = access.getFamily();
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Rasm tanlanmagan");
        if (file.getSize() > MAX_COVER_FILE_SIZE) throw new IllegalArgumentException("Cover rasmi 15 MB dan katta bo'lmasligi kerak");
        if (!ALLOWED_COVER_TYPES.contains(file.getContentType())) throw new IllegalArgumentException("Cover uchun faqat rasm fayllari qabul qilinadi");

        String previousPath = family.getCoverStoredFileName();
        family.setCoverStoredFileName(storageService.storeFamilyCover(familyId, file));
        family.setCoverOriginalFileName(file.getOriginalFilename());
        family.setCoverContentType(file.getContentType());
        Family saved = familyRepository.save(family);
        if (previousPath != null) storageService.delete(previousPath);
        return toDto(saved, access.getAccessRole());
    }

    @Transactional(readOnly = true)
    public CoverFile loadCover(Users currentUser, Long familyId) {
        Family family = authorization.readable(currentUser, familyId).getFamily();
        if (family.getCoverStoredFileName() == null) throw new NotFoundException("Cover rasmi topilmadi");
        return new CoverFile(storageService.loadAsResource(family.getCoverStoredFileName()), family.getCoverContentType(), family.getCoverOriginalFileName());
    }

    @Transactional
    public void deleteCover(Users currentUser, Long familyId) {
        Family family = authorization.owner(currentUser, familyId).getFamily();
        String path = family.getCoverStoredFileName();
        if (path == null) return;
        family.setCoverStoredFileName(null);
        family.setCoverOriginalFileName(null);
        family.setCoverContentType(null);
        familyRepository.save(family);
        storageService.delete(path);
    }

    private FamilyDto toDto(FamilyAccess access) {
        return toDto(access.getFamily(), access.getAccessRole());
    }

    private FamilyDto toDto(Family family, FamilyAccessRole accessRole) {
        FamilyDto dto = familyMapper.toDto(family);
        dto.setAccessRole(accessRole);
        if (family.getCoverStoredFileName() != null) {
            dto.setCoverUrl("/families/%d/cover?v=%s".formatted(family.getId(), Integer.toHexString(family.getCoverStoredFileName().hashCode())));
        }
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record CoverFile(org.springframework.core.io.Resource resource, String contentType, String fileName) { }

}

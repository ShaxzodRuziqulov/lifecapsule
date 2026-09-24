package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Media;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.MediaType;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.MediaRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.service.dto.MediaDto;
import com.example.lifecapsule.service.dto.UpdateMediaDto;
import com.example.lifecapsule.service.mapper.MediaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MediaService {
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime", "video/x-msvideo");

    private final MediaRepository mediaRepository;
    private final PersonRepository personRepository;
    private final FamilyAccessRepository familyAccessRepository;
    private final FamilyRepository familyRepository;
    private final MediaMapper mediaMapper;
    private final StorageService storageService;
    private final PersonService personService;

    @Transactional
    public MediaDto upload(Users currentUser, Long familyId, Long personId, String caption, MultipartFile file) {
        FamilyAccess access = getEditableAccess(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fayl tanlanmagan");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Fayl hajmi 200 MB dan katta bo'lmasligi kerak");
        }

        MediaType type = resolveType(file.getContentType());
        String relativePath = storageService.store(familyId, personId, file);

        Media media = new Media();
        media.setFamily(access.getFamily());
        media.setPerson(person);
        media.setType(type);
        media.setStoredFileName(relativePath);
        media.setOriginalFileName(file.getOriginalFilename());
        media.setContentType(file.getContentType());
        media.setFileSize(file.getSize());
        media.setCaption(trimToNull(caption));

        return toDto(mediaRepository.save(media));
    }

    @Transactional(readOnly = true)
    public List<MediaDto> list(Users currentUser, Long familyId, Long personId) {
        getReadableAccess(currentUser, familyId);
        getPersonEntity(familyId, personId);

        if (!personService.canViewFullProfile(currentUser, familyId, personId)) {
            return List.of();
        }

        return mediaRepository.findAllByPersonIdOrderByCreatedAtAsc(personId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MediaDto updateCaption(Users currentUser, Long familyId, Long personId, Long mediaId, UpdateMediaDto input) {
        getEditableAccess(currentUser, familyId);
        Media media = mediaRepository.findByIdAndPersonIdAndFamilyId(mediaId, personId, familyId)
                .orElseThrow(() -> new NotFoundException("Fayl topilmadi"));

        media.setCaption(trimToNull(input.getCaption()));
        return toDto(mediaRepository.save(media));
    }

    @Transactional
    public void delete(Users currentUser, Long familyId, Long personId, Long mediaId) {
        getEditableAccess(currentUser, familyId);
        Media media = mediaRepository.findByIdAndPersonIdAndFamilyId(mediaId, personId, familyId)
                .orElseThrow(() -> new NotFoundException("Fayl topilmadi"));

        mediaRepository.delete(media);
        storageService.delete(media.getStoredFileName());
    }

    @Transactional(readOnly = true)
    public MediaFile loadFile(Users currentUser, Long familyId, Long personId, Long mediaId) {
        getReadableAccess(currentUser, familyId);
        if (!personService.canViewFullProfile(currentUser, familyId, personId)) {
            throw new NotFoundException("Fayl topilmadi");
        }
        Media media = mediaRepository.findByIdAndPersonIdAndFamilyId(mediaId, personId, familyId)
                .orElseThrow(() -> new NotFoundException("Fayl topilmadi"));

        Resource resource = storageService.loadAsResource(media.getStoredFileName());
        return new MediaFile(resource, media.getContentType(), media.getOriginalFileName());
    }

    private MediaDto toDto(Media media) {
        MediaDto dto = mediaMapper.toDto(media);
        dto.setUrl("/families/%d/persons/%d/media/%d/file".formatted(media.getFamily().getId(), media.getPerson().getId(), media.getId()));
        return dto;
    }

    private MediaType resolveType(String contentType) {
        if (contentType != null && ALLOWED_PHOTO_TYPES.contains(contentType)) {
            return MediaType.PHOTO;
        }
        if (contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return MediaType.VIDEO;
        }
        throw new IllegalArgumentException("Qo'llab-quvvatlanmaydigan fayl turi: " + contentType);
    }

    private Person getPersonEntity(Long familyId, Long personId) {
        return personRepository.findByIdAndFamilyId(personId, familyId)
                .orElseThrow(() -> new NotFoundException("Odam topilmadi"));
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

    private FamilyAccess getEditableAccess(Users currentUser, Long familyId) {
        FamilyAccess access = getReadableAccess(currentUser, familyId);
        if (access.getAccessRole() == FamilyAccessRole.VIEWER) {
            throw new ForbiddenException("Sizda bu odamga fayl qo'shish huquqi yo'q");
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record MediaFile(Resource resource, String contentType, String fileName) {
    }
}

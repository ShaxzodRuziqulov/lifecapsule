package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Media;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.MediaType;
import com.example.lifecapsule.errors.NotFoundException;
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

import java.util.Comparator;
import java.util.HashSet;
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
    private final FamilyAuthorization authorization;
    private final MediaMapper mediaMapper;
    private final StorageService storageService;
    private final PersonService personService;

    @Transactional
    public MediaDto upload(Users currentUser, Long familyId, Long personId, String caption, MultipartFile file) {
        FamilyAccess access = authorization.editable(currentUser, familyId);
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
        authorization.readable(currentUser, familyId);
        getPersonEntity(familyId, personId);

        Set<Long> masked = personService.maskedPersonIds(currentUser, familyId);
        if (masked.contains(personId)) {
            return List.of();
        }

        // The gallery also pulls in tagged and family-wide items owned by other people,
        // so each item is checked against its owner's privacy, not just this person's.
        return mediaRepository.findAllVisibleToPerson(familyId, personId)
                .stream()
                .filter(media -> !masked.contains(media.getPerson().getId()))
                .map(this::toDto)
                .toList();
    }

    /**
     * Every media item in the family the current user is allowed to see - i.e. everything
     * except items owned by a person whose profile is masked for this viewer (mahram privacy).
     */
    @Transactional(readOnly = true)
    public List<MediaDto> listForFamily(Users currentUser, Long familyId) {
        authorization.readable(currentUser, familyId);
        Set<Long> masked = personService.maskedPersonIds(currentUser, familyId);
        return mediaRepository.findAllByFamilyId(familyId).stream()
                .filter(media -> !masked.contains(media.getPerson().getId()))
                .sorted(Comparator.comparing(Media::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MediaDto updateCaption(Users currentUser, Long familyId, Long personId, Long mediaId, UpdateMediaDto input) {
        authorization.editable(currentUser, familyId);
        Media media = mediaRepository.findByIdAndPersonIdAndFamilyId(mediaId, personId, familyId)
                .orElseThrow(() -> new NotFoundException("Fayl topilmadi"));

        media.setCaption(trimToNull(input.getCaption()));
        media.setTaggedPersons(resolveTaggedPersons(familyId, personId, input.getTaggedPersonIds()));
        media.setVisibleToFamily(input.isVisibleToFamily());
        return toDto(mediaRepository.save(media));
    }

    private Set<Person> resolveTaggedPersons(Long familyId, Long ownerPersonId, List<Long> taggedPersonIds) {
        if (taggedPersonIds == null || taggedPersonIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> distinctIds = taggedPersonIds.stream()
                .filter(id -> !id.equals(ownerPersonId))
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Person> people = personRepository.findAllByIdInAndFamilyId(distinctIds, familyId);
        if (people.size() != distinctIds.size()) {
            throw new IllegalArgumentException("Tanlangan odamlardan biri shu oilada topilmadi");
        }
        return new HashSet<>(people);
    }

    @Transactional
    public void delete(Users currentUser, Long familyId, Long personId, Long mediaId) {
        authorization.editable(currentUser, familyId);
        Media media = mediaRepository.findByIdAndPersonIdAndFamilyId(mediaId, personId, familyId)
                .orElseThrow(() -> new NotFoundException("Fayl topilmadi"));

        mediaRepository.delete(media);
        storageService.delete(media.getStoredFileName());    }

    @Transactional(readOnly = true)
    public MediaFile loadFile(Users currentUser, Long familyId, Long personId, Long mediaId) {
        authorization.readable(currentUser, familyId);
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
        dto.setTaggedPersonIds(media.getTaggedPersons().stream().map(Person::getId).toList());
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record MediaFile(Resource resource, String contentType, String fileName) {
    }
}

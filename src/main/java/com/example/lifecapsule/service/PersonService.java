package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.*;
import com.example.lifecapsule.entity.enumirated.*;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.*;
import com.example.lifecapsule.service.dto.*;
import com.example.lifecapsule.service.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PersonService {
    private static final long MAX_AVATAR_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final String DEFAULT_SORT = "firstName";
    private static final Map<String, String> ALLOWED_SORTS = Map.of(
            "firstName", "firstName",
            "lastName", "lastName",
            "birthDate", "birthDate",
            "createdAt", "createdAt"
    );

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final FamilyAuthorization authorization;
    private final UserRepository userRepository;
    private final PersonMapper personMapper;
    private final MediaRepository mediaRepository;
    private final StorageService storageService;

    @Transactional
    public PersonDto createPerson(Users currentUser, Long familyId, CreatePersonDto input) {
        FamilyAccess access = authorization.editable(currentUser, familyId);

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

        return applyAvatarUrl(personMapper.toDto(personRepository.save(person)), person);
    }

    @Transactional
    public PersonDto updatePerson(Users currentUser, Long familyId, Long personId, PersonUpdateDto personDto) {
        if (personId == null) {
            throw new IllegalArgumentException("personId cannot be null");
        }

        authorization.editable(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);

        if (personDto.getBirthDate() != null
                && personDto.getDeathDate() != null
                && personDto.getDeathDate().isBefore(personDto.getBirthDate())) {
            throw new IllegalArgumentException("Vafot etgan sana tug'ilgan sanadan oldin bo'lishi mumkin emas");
        }

        if (personDto.getGender() != person.getGender()) {
            validateGenderChange(familyId, person, personDto.getGender());
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

        Users linkedUser = null;
        if (personDto.getLinkedUserId() != null) {
            linkedUser = userRepository.findById(personDto.getLinkedUserId())
                    .orElseThrow(() -> new NotFoundException("linkedUserId topilmadi"));
        }
        person.setLinkedUser(linkedUser);

        return applyAvatarUrl(personMapper.toDto(personRepository.save(person)), person);
    }

    @Transactional(readOnly = true)
    public PageResponse<PersonDto> getPersons(
            Users currentUser,
            Long familyId,
            PageFilter filter
    ) {
        FamilyAccess access = authorization.readable(currentUser, familyId);
        PersonPrivacyContext privacyContext = createPrivacyContext(currentUser, access);
        Pageable pageable = filter.toPageable(DEFAULT_SORT, ALLOWED_SORTS);

        String search = filter.normalizedQuery();
        var page = searchByFamilyId(familyId, search, pageable, access);

        Page<PersonDto> result = page.map(person -> toDto(person, privacyContext));
        return new PageResponse<>(result);
    }

    @Transactional(readOnly = true)
    public PersonDto getPerson(Users currentUser, Long familyId, Long personId) {
        FamilyAccess access = authorization.readable(currentUser, familyId);
        PersonPrivacyContext privacyContext = createPrivacyContext(currentUser, access);
        return toDto(getPersonEntity(familyId, personId), privacyContext);
    }

    /**
     * Whether currentUser may see this person's private details (and by extension, their media) -
     * false for the same "mahram" masking applied to sensitive female profiles for VIEWER access.
     */
    @Transactional(readOnly = true)
    public boolean canViewFullProfile(Users currentUser, Long familyId, Long personId) {
        FamilyAccess access = authorization.readable(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);
        PersonPrivacyContext privacyContext = createPrivacyContext(currentUser, access);
        return !shouldMaskSensitiveProfile(person, privacyContext);
    }

    /** Ids of everyone in the family whose profile (and media) is masked for currentUser. */
    @Transactional(readOnly = true)
    public Set<Long> maskedPersonIds(Users currentUser, Long familyId) {
        FamilyAccess access = authorization.readable(currentUser, familyId);
        if (access.getAccessRole() != FamilyAccessRole.VIEWER) {
            return Set.of();
        }
        PersonPrivacyContext privacyContext = createPrivacyContext(currentUser, access);
        Set<Long> masked = new HashSet<>(personRepository.findIdsByFamilyIdAndGender(familyId, Gender.FEMALE));
        masked.removeAll(privacyContext.visibleSensitivePersonIds());
        return masked;
    }

    @Transactional
    public void deletePerson(Users currentUser, Long familyId, Long personId) {
        authorization.editable(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);
        List<Media> media = mediaRepository.findAllByPersonId(personId);
        String avatarPath = person.getAvatarStoredFileName();
        relationshipRepository.deleteAllByFamilyIdAndPersonId(familyId, personId);
        mediaRepository.clearTagsForPerson(personId);
        mediaRepository.deleteAllByPersonId(personId);
        personRepository.delete(person);
        media.forEach(item -> storageService.delete(item.getStoredFileName()));
        if (avatarPath != null) storageService.delete(avatarPath);
    }

    @Transactional
    public PersonDto uploadAvatar(Users currentUser, Long familyId, Long personId, MultipartFile file) {
        authorization.editable(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Rasm tanlanmagan");
        if (file.getSize() > MAX_AVATAR_FILE_SIZE) throw new IllegalArgumentException("Profil rasmi 10 MB dan katta bo'lmasligi kerak");
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) throw new IllegalArgumentException("Profil uchun faqat rasm fayllari qabul qilinadi");

        String previousPath = person.getAvatarStoredFileName();
        person.setAvatarStoredFileName(storageService.storePersonAvatar(familyId, personId, file));
        person.setAvatarOriginalFileName(file.getOriginalFilename());
        person.setAvatarContentType(file.getContentType());
        Person saved = personRepository.save(person);
        if (previousPath != null) storageService.delete(previousPath);
        return applyAvatarUrl(personMapper.toDto(saved), saved);
    }

    @Transactional(readOnly = true)
    public AvatarFile loadAvatar(Users currentUser, Long familyId, Long personId) {
        if (!canViewFullProfile(currentUser, familyId, personId)) throw new NotFoundException("Profil rasmi topilmadi");
        Person person = getPersonEntity(familyId, personId);
        if (person.getAvatarStoredFileName() == null) throw new NotFoundException("Profil rasmi topilmadi");
        return new AvatarFile(storageService.loadAsResource(person.getAvatarStoredFileName()), person.getAvatarContentType(), person.getAvatarOriginalFileName());
    }

    @Transactional
    public void deleteAvatar(Users currentUser, Long familyId, Long personId) {
        authorization.editable(currentUser, familyId);
        Person person = getPersonEntity(familyId, personId);
        String path = person.getAvatarStoredFileName();
        if (path == null) return;
        person.setAvatarStoredFileName(null);
        person.setAvatarOriginalFileName(null);
        person.setAvatarContentType(null);
        personRepository.save(person);
        storageService.delete(path);
    }

    /** Changing a parent's gender must not give one of their children two fathers or two mothers. */
    private void validateGenderChange(Long familyId, Person person, Gender newGender) {
        if (newGender != Gender.MALE && newGender != Gender.FEMALE) return;
        List<Relationship> relationships = relationshipRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId);
        for (Relationship asParent : relationships) {
            if (asParent.getType() == RelationshipType.PARTNER || !asParent.getFromPerson().getId().equals(person.getId())) continue;
            Person child = asParent.getToPerson();
            boolean clash = relationships.stream().anyMatch(other -> other.getType() == asParent.getType()
                    && other.getToPerson().getId().equals(child.getId())
                    && !other.getFromPerson().getId().equals(person.getId())
                    && other.getFromPerson().getGender() == newGender);
            if (clash) {
                throw new ConflictException(child.getFirstName() + "ning " + (newGender == Gender.MALE ? "otasi" : "onasi")
                        + " allaqachon kiritilgan, shuning uchun jinsni o'zgartirib bo'lmaydi");
            }
        }
    }

    private Person getPersonEntity(Long familyId, Long personId) {
        return personRepository.findByIdAndFamilyId(personId, familyId)
                .orElseThrow(() -> new NotFoundException("Odam topilmadi"));
    }

    private PersonDto toDto(Person person, PersonPrivacyContext privacyContext) {
        PersonDto dto = applyAvatarUrl(personMapper.toDto(person), person);
        if (shouldMaskSensitiveProfile(person, privacyContext)) {
            maskSensitiveProfile(dto);
        }
        return dto;
    }

    private Page<Person> searchByFamilyId(
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
        dto.setAvatarUrl(null);
        dto.setLinkedUserId(null);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PersonDto applyAvatarUrl(PersonDto dto, Person person) {
        if (person.getAvatarStoredFileName() != null) {
            // The version changes with every upload so clients can cache the image by URL.
            dto.setAvatarUrl("/families/%d/persons/%d/avatar?v=%s".formatted(
                    person.getFamily().getId(), person.getId(), Integer.toHexString(person.getAvatarStoredFileName().hashCode())));
        }
        return dto;
    }

    public record AvatarFile(org.springframework.core.io.Resource resource, String contentType, String fileName) { }

    private record PersonPrivacyContext(FamilyAccess access, Set<Long> visibleSensitivePersonIds) {
    }

}

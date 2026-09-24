package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Media;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.MediaType;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.MediaRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.service.dto.MediaDto;
import com.example.lifecapsule.service.dto.UpdateMediaDto;
import com.example.lifecapsule.service.mapper.MediaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    @Mock private MediaRepository mediaRepository;
    @Mock private PersonRepository personRepository;
    @Mock private FamilyAccessRepository familyAccessRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private MediaMapper mediaMapper;
    @Mock private StorageService storageService;
    @Mock private PersonService personService;
    @InjectMocks private MediaService mediaService;

    private final Users editor = user(2L);
    private final Family family = family(10L);
    private Media media;

    @BeforeEach void setup() {
        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(access(FamilyAccessRole.EDITOR)));
        media = media(1L, 30L);
    }

    @Test void tagsOtherFamilyMembers() {
        stubExistingMedia();
        when(personRepository.findAllByIdInAndFamilyId(List.of(40L, 50L), 10L))
                .thenReturn(List.of(person(40L), person(50L)));

        MediaDto result = mediaService.updateCaption(editor, 10L, 30L, 1L, update("Piknik", 40L, 50L));

        assertThat(result.getTaggedPersonIds()).containsExactlyInAnyOrder(40L, 50L);
        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getTaggedPersons()).extracting(Person::getId).containsExactlyInAnyOrder(40L, 50L);
    }

    @Test void dropsTheOwnerFromTheirOwnTagList() {
        stubExistingMedia();
        when(personRepository.findAllByIdInAndFamilyId(List.of(40L), 10L)).thenReturn(List.of(person(40L)));

        MediaDto result = mediaService.updateCaption(editor, 10L, 30L, 1L, update("Piknik", 30L, 40L));

        assertThat(result.getTaggedPersonIds()).containsExactly(40L);
    }

    @Test void clearsTagsWhenListIsEmpty() {
        stubExistingMedia();
        media.getTaggedPersons().add(person(40L));

        MediaDto result = mediaService.updateCaption(editor, 10L, 30L, 1L, update("Piknik"));

        assertThat(result.getTaggedPersonIds()).isEmpty();
        verify(personRepository, never()).findAllByIdInAndFamilyId(any(), any());
    }

    @Test void rejectsATagFromOutsideTheFamily() {
        when(mediaRepository.findByIdAndPersonIdAndFamilyId(1L, 30L, 10L)).thenReturn(Optional.of(media));
        when(personRepository.findAllByIdInAndFamilyId(List.of(99L), 10L)).thenReturn(List.of());

        assertThatThrownBy(() -> mediaService.updateCaption(editor, 10L, 30L, 1L, update("Piknik", 99L)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mediaRepository, never()).save(any());
    }

    @Test void listReturnsMediaOwnedOrTaggedAndMapsTagIds() {
        Media tagged = media(2L, 99L);
        tagged.getTaggedPersons().add(person(30L));
        stubToDto();
        when(personRepository.findByIdAndFamilyId(30L, 10L)).thenReturn(Optional.of(person(30L)));
        when(personService.canViewFullProfile(editor, 10L, 30L)).thenReturn(true);
        when(mediaRepository.findAllVisibleToPerson(10L, 30L)).thenReturn(List.of(media, tagged));

        List<MediaDto> result = mediaService.list(editor, 10L, 30L);

        assertThat(result).extracting(MediaDto::getId).containsExactly(1L, 2L);
        assertThat(result.get(1).getTaggedPersonIds()).containsExactly(30L);
    }

    @Test void viewerCannotTagPeople() {
        FamilyAccess viewerAccess = access(FamilyAccessRole.VIEWER);
        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 2L)).thenReturn(Optional.of(viewerAccess));

        assertThatThrownBy(() -> mediaService.updateCaption(editor, 10L, 30L, 1L, update("Piknik", 40L)))
                .isInstanceOf(com.example.lifecapsule.errors.ForbiddenException.class);
        verify(mediaRepository, never()).save(any());
    }

    private void stubExistingMedia() {
        when(mediaRepository.findByIdAndPersonIdAndFamilyId(1L, 30L, 10L)).thenReturn(Optional.of(media));
        when(mediaRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        stubToDto();
    }

    private void stubToDto() {
        when(mediaMapper.toDto(any())).thenAnswer(call -> {
            Media source = call.getArgument(0);
            MediaDto dto = new MediaDto();
            dto.setId(source.getId());
            dto.setCaption(source.getCaption());
            return dto;
        });
    }

    private UpdateMediaDto update(String caption, Long... taggedPersonIds) {
        UpdateMediaDto dto = new UpdateMediaDto();
        dto.setCaption(caption);
        dto.setTaggedPersonIds(taggedPersonIds.length == 0 ? List.of() : List.of(taggedPersonIds));
        return dto;
    }

    private Users user(Long id) {
        Users user = new Users();
        user.setId(id);
        return user;
    }

    private Family family(Long id) {
        Family family = new Family();
        family.setId(id);
        return family;
    }

    private FamilyAccess access(FamilyAccessRole role) {
        FamilyAccess access = new FamilyAccess();
        access.setFamily(family);
        access.setUser(editor);
        access.setAccessRole(role);
        access.setStatus(AccessStatus.ACTIVE);
        return access;
    }

    private Person person(Long id) {
        Person person = new Person();
        person.setId(id);
        person.setFamily(family);
        return person;
    }

    private Media media(Long id, Long ownerId) {
        Media media = new Media();
        media.setId(id);
        media.setFamily(family);
        media.setPerson(person(ownerId));
        media.setType(MediaType.PHOTO);
        media.setStoredFileName("stored.jpg");
        media.setOriginalFileName("original.jpg");
        media.setContentType("image/jpeg");
        media.setFileSize(100L);
        return media;
    }
}

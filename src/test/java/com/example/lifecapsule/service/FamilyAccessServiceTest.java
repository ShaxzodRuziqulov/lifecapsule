package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.CreateFamilyAccessDto;
import com.example.lifecapsule.service.dto.FamilyAccessDto;
import com.example.lifecapsule.service.mapper.FamilyAccessMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyAccessServiceTest {
    @Mock
    private FamilyAccessRepository familyAccessRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FamilyAccessMapper familyAccessMapper;
    @InjectMocks
    private FamilyAccessService familyAccessService;

    @Test
    void editorCannotManageFamilyAccess() {
        Users editor = user(2L, "editor@lifecapsule.uz");
        Family family = family(10L);
        when(familyAccessRepository.findByFamilyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(access(family, editor, FamilyAccessRole.EDITOR)));

        CreateFamilyAccessDto input = new CreateFamilyAccessDto();
        input.setEmail("viewer@lifecapsule.uz");
        input.setAccessRole(FamilyAccessRole.VIEWER);

        assertThatThrownBy(() -> familyAccessService.addFamilyAccess(editor, 10L, input))
                .isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void ownerCanGrantViewerAccess() {
        Users owner = user(1L, "owner@lifecapsule.uz");
        Users viewer = user(3L, "viewer@lifecapsule.uz");
        Family family = family(10L);
        FamilyAccess ownerAccess = access(family, owner, FamilyAccessRole.OWNER);
        when(familyAccessRepository.findByFamilyIdAndUserId(any(), any()))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(1);
                    return userId.equals(1L) ? Optional.of(ownerAccess) : Optional.empty();
                });
        when(userRepository.findByEmailIgnoreCase("viewer@lifecapsule.uz"))
                .thenReturn(Optional.of(viewer));
        when(familyAccessRepository.save(any(FamilyAccess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FamilyAccessDto dto = new FamilyAccessDto();
        dto.setUserId(3L);
        dto.setAccessRole(FamilyAccessRole.VIEWER);
        when(familyAccessMapper.toDto(any(FamilyAccess.class))).thenReturn(dto);

        FamilyAccessDto result = familyAccessService.addFamilyAccess(owner, 10L, input("viewer@lifecapsule.uz"));

        assertThat(result.getUserId()).isEqualTo(3L);
        ArgumentCaptor<FamilyAccess> captor = ArgumentCaptor.forClass(FamilyAccess.class);
        verify(familyAccessRepository).save(captor.capture());
        assertThat(captor.getValue().getAccessRole()).isEqualTo(FamilyAccessRole.VIEWER);
        assertThat(captor.getValue().getStatus()).isEqualTo(AccessStatus.ACTIVE);
    }

    @Test
    void ownerCanCreatePendingInvitation() {
        Users owner = user(1L, "owner@lifecapsule.uz");
        Users viewer = user(3L, "viewer@lifecapsule.uz");
        Family family = family(10L);
        FamilyAccess ownerAccess = access(family, owner, FamilyAccessRole.OWNER);
        when(familyAccessRepository.findByFamilyIdAndUserId(any(), any()))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(1);
                    return userId.equals(1L) ? Optional.of(ownerAccess) : Optional.empty();
                });
        when(userRepository.findByEmailIgnoreCase("viewer@lifecapsule.uz"))
                .thenReturn(Optional.of(viewer));
        when(familyAccessRepository.save(any(FamilyAccess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(familyAccessMapper.toDto(any(FamilyAccess.class))).thenReturn(new FamilyAccessDto());

        familyAccessService.inviteFamilyAccess(owner, 10L, input("viewer@lifecapsule.uz"));

        ArgumentCaptor<FamilyAccess> captor = ArgumentCaptor.forClass(FamilyAccess.class);
        verify(familyAccessRepository).save(captor.capture());
        assertThat(captor.getValue().getAccessRole()).isEqualTo(FamilyAccessRole.VIEWER);
        assertThat(captor.getValue().getStatus()).isEqualTo(AccessStatus.PENDING);
    }

    @Test
    void invitedUserCanAcceptInvitation() {
        Users viewer = user(3L, "viewer@lifecapsule.uz");
        FamilyAccess access = access(family(10L), viewer, FamilyAccessRole.VIEWER);
        access.setId(20L);
        access.setStatus(AccessStatus.PENDING);
        when(familyAccessRepository.findById(20L)).thenReturn(Optional.of(access));
        when(familyAccessRepository.save(access)).thenReturn(access);
        FamilyAccessDto dto = new FamilyAccessDto();
        dto.setId(20L);
        dto.setStatus(AccessStatus.ACTIVE);
        when(familyAccessMapper.toDto(access)).thenReturn(dto);

        FamilyAccessDto result = familyAccessService.acceptInvitation(viewer, 20L);

        assertThat(result.getStatus()).isEqualTo(AccessStatus.ACTIVE);
        assertThat(access.getStatus()).isEqualTo(AccessStatus.ACTIVE);
    }

    private CreateFamilyAccessDto input(String email) {
        CreateFamilyAccessDto input = new CreateFamilyAccessDto();
        input.setEmail(email);
        input.setAccessRole(FamilyAccessRole.VIEWER);
        return input;
    }

    private Users user(Long id, String email) {
        Users user = new Users();
        user.setId(id);
        user.setEmail(email);
        user.setUserName(email);
        return user;
    }

    private Family family(Long id) {
        Family family = new Family();
        family.setId(id);
        family.setName("Demo");
        return family;
    }

    private FamilyAccess access(Family family, Users user, FamilyAccessRole role) {
        FamilyAccess access = new FamilyAccess();
        access.setFamily(family);
        access.setUser(user);
        access.setAccessRole(role);
        access.setStatus(AccessStatus.ACTIVE);
        return access;
    }
}

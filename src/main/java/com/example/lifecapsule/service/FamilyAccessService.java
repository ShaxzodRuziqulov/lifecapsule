package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.dto.CreateFamilyAccessDto;
import com.example.lifecapsule.service.dto.FamilyAccessDto;
import com.example.lifecapsule.service.dto.UpdateFamilyAccessDto;
import com.example.lifecapsule.service.mapper.FamilyAccessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FamilyAccessService {
    private final FamilyAccessRepository familyAccessRepository;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final FamilyAccessMapper familyAccessMapper;

    @Transactional(readOnly = true)
    public List<FamilyAccessDto> getFamilyAccesses(Users currentUser, Long familyId) {
        getOwnerAccess(currentUser, familyId);
        return familyAccessRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId)
                .stream()
                .map(familyAccessMapper::toDto)
                .toList();
    }

    @Transactional
    public FamilyAccessDto addFamilyAccess(Users currentUser, Long familyId, CreateFamilyAccessDto input) {
        FamilyAccess ownerAccess = getOwnerAccess(currentUser, familyId);
        Users user = resolveUser(currentUser, input);

        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(familyId, user.getId())
                .orElseGet(FamilyAccess::new);

        access.setFamily(ownerAccess.getFamily());
        access.setUser(user);
        access.setAccessRole(input.getAccessRole() == null ? FamilyAccessRole.VIEWER : input.getAccessRole());
        access.setStatus(AccessStatus.ACTIVE);

        return familyAccessMapper.toDto(familyAccessRepository.save(access));
    }

    @Transactional
    public FamilyAccessDto inviteFamilyAccess(Users currentUser, Long familyId, CreateFamilyAccessDto input) {
        FamilyAccess ownerAccess = getOwnerAccess(currentUser, familyId);
        Users user = resolveUser(currentUser, input);

        Optional<FamilyAccess> existingAccess = familyAccessRepository.findByFamilyIdAndUserId(familyId, user.getId());
        if (existingAccess.isPresent() && existingAccess.get().getStatus() == AccessStatus.ACTIVE) {
            throw new ConflictException("Bu foydalanuvchida oilaga ruxsat allaqachon mavjud");
        }

        FamilyAccess access = existingAccess.orElseGet(FamilyAccess::new);
        access.setFamily(ownerAccess.getFamily());
        access.setUser(user);
        access.setAccessRole(input.getAccessRole() == null ? FamilyAccessRole.VIEWER : input.getAccessRole());
        access.setStatus(AccessStatus.PENDING);

        return familyAccessMapper.toDto(familyAccessRepository.save(access));
    }

    @Transactional(readOnly = true)
    public List<FamilyAccessDto> getMyInvitations(Users currentUser) {
        return familyAccessRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        AccessStatus.PENDING
                )
                .stream()
                .map(familyAccessMapper::toDto)
                .toList();
    }

    @Transactional
    public FamilyAccessDto acceptInvitation(Users currentUser, Long accessId) {
        FamilyAccess access = getInvitationForUser(currentUser, accessId);
        access.setStatus(AccessStatus.ACTIVE);
        return familyAccessMapper.toDto(familyAccessRepository.save(access));
    }

    @Transactional
    public FamilyAccessDto rejectInvitation(Users currentUser, Long accessId) {
        FamilyAccess access = getInvitationForUser(currentUser, accessId);
        access.setStatus(AccessStatus.REMOVED);
        return familyAccessMapper.toDto(familyAccessRepository.save(access));
    }

    private Users resolveUser(Users currentUser, CreateFamilyAccessDto input) {

        Users user;
        if (input.getUserId() != null) {
            user = userRepository.findById(input.getUserId())
                    .orElseThrow(() -> new NotFoundException("User topilmadi"));
        } else if (input.getEmail() != null && !input.getEmail().isBlank()) {
            user = userRepository.findByEmailIgnoreCase(input.getEmail().trim())
                    .orElseThrow(() -> new NotFoundException("Bu email bilan foydalanuvchi topilmadi"));
        } else {
            throw new IllegalArgumentException("Foydalanuvchi emaili kiritilishi kerak");
        }

        if (currentUser.getId().equals(user.getId())) {
            throw new IllegalArgumentException("O'zingizga qayta ruxsat bera olmaysiz");
        }

        return user;
    }

    @Transactional
    public FamilyAccessDto updateFamilyAccess(
            Users currentUser,
            Long familyId,
            Long accessId,
            UpdateFamilyAccessDto input
    ) {
        getOwnerAccess(currentUser, familyId);
        FamilyAccess access = getAccessEntity(familyId, accessId);

        if (!isAdmin(currentUser) && access.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Owner o'z ruxsatini o'zgartira olmaydi");
        }

        access.setAccessRole(input.getAccessRole());
        access.setStatus(input.getStatus());
        return familyAccessMapper.toDto(familyAccessRepository.save(access));
    }

    @Transactional
    public void removeFamilyAccess(Users currentUser, Long familyId, Long accessId) {
        getOwnerAccess(currentUser, familyId);
        FamilyAccess access = getAccessEntity(familyId, accessId);

        if (!isAdmin(currentUser) && access.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Owner o'z ruxsatini o'chira olmaydi");
        }

        access.setStatus(AccessStatus.REMOVED);
        familyAccessRepository.save(access);
    }

    private FamilyAccess getOwnerAccess(Users currentUser, Long familyId) {
        if (isAdmin(currentUser)) {
            return adminAccess(currentUser, familyId);
        }

        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(familyId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q"));

        if (access.getStatus() != AccessStatus.ACTIVE || access.getAccessRole() != FamilyAccessRole.OWNER) {
            throw new ForbiddenException("Bu amalni faqat oila egasi bajaradi");
        }
        return access;
    }

    private FamilyAccess adminAccess(Users currentUser, Long familyId) {
        var family = familyRepository.findById(familyId)
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

    private FamilyAccess getAccessEntity(Long familyId, Long accessId) {
        return familyAccessRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId)
                .stream()
                .filter(access -> access.getId().equals(accessId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Ruxsat topilmadi"));
    }

    private FamilyAccess getInvitationForUser(Users currentUser, Long accessId) {
        FamilyAccess access = familyAccessRepository.findById(accessId)
                .orElseThrow(() -> new NotFoundException("Taklif topilmadi"));

        if (!access.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bu taklif sizga tegishli emas");
        }
        if (access.getStatus() != AccessStatus.PENDING) {
            throw new ConflictException("Taklif faol holatda emas");
        }
        return access;
    }
}

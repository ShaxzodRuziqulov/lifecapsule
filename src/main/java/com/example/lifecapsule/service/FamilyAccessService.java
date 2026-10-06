package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
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
    private final FamilyAuthorization authorization;
    private final UserRepository userRepository;
    private final FamilyAccessMapper familyAccessMapper;

    @Transactional(readOnly = true)
    public List<FamilyAccessDto> getFamilyAccesses(Users currentUser, Long familyId) {
        authorization.owner(currentUser, familyId);
        return familyAccessRepository.findAllByFamilyIdOrderByCreatedAtAsc(familyId)
                .stream()
                .map(familyAccessMapper::toDto)
                .toList();
    }

    @Transactional
    public FamilyAccessDto inviteFamilyAccess(Users currentUser, Long familyId, CreateFamilyAccessDto input) {
        FamilyAccess ownerAccess = authorization.owner(currentUser, familyId);
        Users user = resolveUser(currentUser, input);
        FamilyAccessRole role = requireNonOwnerRole(input.getAccessRole());

        Optional<FamilyAccess> existingAccess = familyAccessRepository.findByFamilyIdAndUserId(familyId, user.getId());
        if (existingAccess.isPresent() && existingAccess.get().getStatus() == AccessStatus.ACTIVE) {
            throw new ConflictException("Bu foydalanuvchida oilaga ruxsat allaqachon mavjud");
        }

        FamilyAccess access = existingAccess.orElseGet(FamilyAccess::new);
        access.setFamily(ownerAccess.getFamily());
        access.setUser(user);
        access.setAccessRole(role);
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

    @Transactional(readOnly = true)
    public List<FamilyAccessDto> getMyInvitationHistory(Users currentUser) {
        return familyAccessRepository.findMyInvitationHistory(currentUser.getId())
                .stream()
                .map(familyAccessMapper::toDto)
                .toList();
    }

    private FamilyAccessRole requireNonOwnerRole(FamilyAccessRole role) {
        if (role == null) {
            return FamilyAccessRole.VIEWER;
        }
        if (role == FamilyAccessRole.OWNER) {
            throw new ForbiddenException("Oila egaligini bu yo'l bilan berib bo'lmaydi");
        }
        return role;
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
        authorization.owner(currentUser, familyId);
        FamilyAccess access = getAccessEntity(familyId, accessId);

        if (!FamilyAuthorization.isAdmin(currentUser) && access.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Owner o'z ruxsatini o'zgartira olmaydi");
        }

        if (input.getStatus() == AccessStatus.ACTIVE && access.getStatus() != AccessStatus.ACTIVE) {
            throw new ConflictException("Ruxsat faqat foydalanuvchi taklifni qabul qilganda faollashadi");
        }
        access.setAccessRole(requireNonOwnerRole(input.getAccessRole()));
        access.setStatus(input.getStatus());
        return familyAccessMapper.toDto(familyAccessRepository.save(access));
    }

    @Transactional
    public void removeFamilyAccess(Users currentUser, Long familyId, Long accessId) {
        authorization.owner(currentUser, familyId);
        FamilyAccess access = getAccessEntity(familyId, accessId);

        if (!FamilyAuthorization.isAdmin(currentUser) && access.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Owner o'z ruxsatini o'chira olmaydi");
        }

        access.setStatus(AccessStatus.REMOVED);
        familyAccessRepository.save(access);
    }

    private FamilyAccess getAccessEntity(Long familyId, Long accessId) {
        return familyAccessRepository.findByIdAndFamilyId(accessId, familyId)
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

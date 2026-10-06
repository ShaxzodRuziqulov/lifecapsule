package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The single place that decides what a user may do inside a family. An ADMIN acts as the
 * owner of every family; everyone else needs an ACTIVE access row. A missing or inactive row
 * is reported as "not found" so outsiders cannot probe which families exist.
 */
@Component
@RequiredArgsConstructor
public class FamilyAuthorization {
    private final FamilyAccessRepository familyAccessRepository;
    private final FamilyRepository familyRepository;

    public static boolean isAdmin(Users user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public FamilyAccess readable(Users user, Long familyId) {
        if (isAdmin(user)) {
            return adminAccess(user, familyId);
        }
        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(familyId, user.getId())
                .orElseThrow(FamilyAuthorization::notFound);
        if (access.getStatus() != AccessStatus.ACTIVE) {
            throw notFound();
        }
        return access;
    }

    public FamilyAccess editable(Users user, Long familyId) {
        FamilyAccess access = readable(user, familyId);
        if (access.getAccessRole() == FamilyAccessRole.VIEWER) {
            throw new ForbiddenException("Sizda bu oilani o'zgartirish huquqi yo'q");
        }
        return access;
    }

    public FamilyAccess owner(Users user, Long familyId) {
        FamilyAccess access = readable(user, familyId);
        if (access.getAccessRole() != FamilyAccessRole.OWNER) {
            throw new ForbiddenException("Bu amalni faqat oila egasi bajaradi");
        }
        return access;
    }

    private FamilyAccess adminAccess(Users admin, Long familyId) {
        FamilyAccess access = new FamilyAccess();
        access.setFamily(familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Oila topilmadi")));
        access.setUser(admin);
        access.setAccessRole(FamilyAccessRole.OWNER);
        access.setStatus(AccessStatus.ACTIVE);
        return access;
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Oila topilmadi yoki sizda ruxsat yo'q");
    }
}

package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyAccessRepository extends JpaRepository<FamilyAccess, Long> {
    Optional<FamilyAccess> findByFamilyIdAndUserId(Long familyId, Long userId);

    List<FamilyAccess> findAllByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AccessStatus status);

    Page<FamilyAccess> findAllByUserIdAndStatus(Long userId, AccessStatus status, Pageable pageable);

    @Query("""
            select access from FamilyAccess access
            where access.user.id = :userId
              and access.status = :status
              and (
                    lower(access.family.name) like lower(concat('%', :q, '%'))
                    or lower(coalesce(access.family.description, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<FamilyAccess> searchMyFamilies(
            @Param("userId") Long userId,
            @Param("status") AccessStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    List<FamilyAccess> findAllByFamilyIdOrderByCreatedAtAsc(Long familyId);

    void deleteAllByFamilyId(Long familyId);
}

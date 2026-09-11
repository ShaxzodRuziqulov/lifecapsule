package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.Family;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select family from Family family where family.id = :familyId")
    Optional<Family> findByIdForUpdate(@Param("familyId") Long familyId);

    List<Family> findAllByCreatedByIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        select family
        from Family family
        where cast(:q as string) is null
           or lower(family.name) like lower(concat('%', cast(:q as string), '%'))
           or lower(coalesce(family.description, '')) like lower(concat('%', cast(:q as string), '%'))
        """)
    Page<Family> searchAllFamiliesPaging(
            @Param("q") String q,
            Pageable pageable
    );
}

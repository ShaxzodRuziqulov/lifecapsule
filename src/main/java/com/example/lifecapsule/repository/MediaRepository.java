package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findAllByPersonIdOrderByCreatedAtAsc(Long personId);

    Optional<Media> findByIdAndPersonIdAndFamilyId(Long id, Long personId, Long familyId);

    List<Media> findAllByFamilyId(Long familyId);

    void deleteAllByPersonId(Long personId);

    void deleteAllByFamilyId(Long familyId);
}

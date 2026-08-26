package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {
    List<Family> findAllByCreatedByIdOrderByCreatedAtDesc(Long userId);
}

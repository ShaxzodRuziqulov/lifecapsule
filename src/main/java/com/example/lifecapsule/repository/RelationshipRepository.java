package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.enumirated.RelationshipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {
    List<Relationship> findAllByFamilyIdOrderByCreatedAtAsc(Long familyId);

    @Query("""
            select relationship from Relationship relationship
            where relationship.family.id = :familyId
              and (cast(:q as string) is null or (
                    lower(coalesce(relationship.note, '')) like lower(concat('%', cast(:q as string), '%'))
                    or lower(relationship.fromPerson.firstName) like lower(concat('%', cast(:q as string), '%'))
                    or lower(coalesce(relationship.fromPerson.lastName, '')) like lower(concat('%', cast(:q as string), '%'))
                    or lower(relationship.toPerson.firstName) like lower(concat('%', cast(:q as string), '%'))
                    or lower(coalesce(relationship.toPerson.lastName, '')) like lower(concat('%', cast(:q as string), '%'))
              ))
            """)
    Page<Relationship> searchByFamilyId(
            @Param("familyId") Long familyId,
            @Param("q") String q,
            Pageable pageable
    );

    Optional<Relationship> findByIdAndFamilyId(Long id, Long familyId);

    @Modifying
    @Query("delete from Relationship relationship where relationship.family.id = :familyId")
    void deleteAllByFamilyId(@Param("familyId") Long familyId);

    @Modifying
    @Query("""
            delete from Relationship relationship
            where relationship.family.id = :familyId
              and (relationship.fromPerson.id = :personId or relationship.toPerson.id = :personId)
            """)
    void deleteAllByFamilyIdAndPersonId(@Param("familyId") Long familyId, @Param("personId") Long personId);

    boolean existsByFamilyIdAndFromPersonIdAndToPersonIdAndType(
            Long familyId,
            Long fromPersonId,
            Long toPersonId,
            RelationshipType type
    );

    boolean existsByFamilyIdAndFromPersonIdAndToPersonIdAndTypeOrFamilyIdAndFromPersonIdAndToPersonIdAndType(
            Long firstFamilyId,
            Long firstFromPersonId,
            Long firstToPersonId,
            RelationshipType firstType,
            Long secondFamilyId,
            Long secondFromPersonId,
            Long secondToPersonId,
            RelationshipType secondType
    );
}

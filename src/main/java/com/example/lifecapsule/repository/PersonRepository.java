package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.Person;
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
public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findAllByFamilyIdOrderByFirstNameAsc(Long familyId);

    Page<Person> findAllByFamilyId(Long familyId, Pageable pageable);

    @Query("""
            select person from Person person
            where person.family.id = :familyId
              and (
                    lower(person.firstName) like lower(concat('%', :q, '%'))
                    or lower(coalesce(person.lastName, '')) like lower(concat('%', :q, '%'))
                    or lower(coalesce(person.maidenName, '')) like lower(concat('%', :q, '%'))
                    or lower(coalesce(person.birthPlace, '')) like lower(concat('%', :q, '%'))
                    or lower(coalesce(person.occupation, '')) like lower(concat('%', :q, '%'))
                    or lower(coalesce(person.biography, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<Person> searchByFamilyId(
            @Param("familyId") Long familyId,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            select person from Person person
            where person.family.id = :familyId
              and (
                    lower(person.firstName) like lower(concat('%', :q, '%'))
                    or lower(coalesce(person.lastName, '')) like lower(concat('%', :q, '%'))
              )
            """)
    Page<Person> searchBasicByFamilyId(
            @Param("familyId") Long familyId,
            @Param("q") String q,
            Pageable pageable
    );

    Optional<Person> findByIdAndFamilyId(Long id, Long familyId);

    Optional<Person> findByFamilyIdAndLinkedUserId(Long familyId, Long linkedUserId);

    @Modifying
    @Query("delete from Person person where person.family.id = :familyId")
    void deleteAllByFamilyId(@Param("familyId") Long familyId);
}

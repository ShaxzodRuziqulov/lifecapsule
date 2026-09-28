package com.example.lifecapsule.repository;

import com.example.lifecapsule.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    @Query("""
            select distinct media from Media media
            left join media.taggedPersons tagged
            where media.family.id = :familyId
              and (media.person.id = :personId or tagged.id = :personId or media.visibleToFamily = true)
            order by media.createdAt asc
            """)
    List<Media> findAllVisibleToPerson(@Param("familyId") Long familyId, @Param("personId") Long personId);

    Optional<Media> findByIdAndPersonIdAndFamilyId(Long id, Long personId, Long familyId);

    List<Media> findAllByPersonId(Long personId);

    List<Media> findAllByFamilyId(Long familyId);

    void deleteAllByPersonId(Long personId);

    void deleteAllByFamilyId(Long familyId);

    /**
     * Clears media_tags rows that would otherwise dangle (FK violation) once this person - and
     * the media they own - are deleted: rows where they're tagged in someone else's media, and
     * rows where someone else is tagged in media they own. Must run before deleteAllByPersonId
     * and before the Person row itself is deleted.
     */
    @Modifying
    @Query(value = """
            delete from media_tags
            where person_id = :personId
               or media_id in (select id from media where person_id = :personId)
            """, nativeQuery = true)
    void clearTagsForPerson(@Param("personId") Long personId);

    /**
     * Same as clearTagsForPerson, scoped to every person/media in a family. Must run before
     * personRepository.deleteAllByFamilyId and mediaRepository.deleteAllByFamilyId.
     */
    @Modifying
    @Query(value = """
            delete from media_tags
            where person_id in (select id from persons where family_id = :familyId)
               or media_id in (select id from media where family_id = :familyId)
            """, nativeQuery = true)
    void clearTagsForFamily(@Param("familyId") Long familyId);
}

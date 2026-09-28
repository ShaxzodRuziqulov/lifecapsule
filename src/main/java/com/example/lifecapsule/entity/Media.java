package com.example.lifecapsule.entity;

import com.example.lifecapsule.entity.enumirated.MediaType;
import com.example.lifecapsule.entity.teamplate.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "media",
        indexes = {
                @Index(name = "idx_media_person", columnList = "person_id"),
                @Index(name = "idx_media_family", columnList = "family_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Media extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * Other family members also shown in this photo/video, besides the owning {@link #person}.
     * Tagging makes the item appear in their galleries too (see MediaRepository.findAllVisibleToPerson).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "media_tags",
            joinColumns = @JoinColumn(name = "media_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Set<Person> taggedPersons = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(nullable = false, length = 300)
    private String storedFileName;

    @Column(nullable = false, length = 300)
    private String originalFileName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 300)
    private String caption;

    /**
     * When true, every family member sees this item in their own gallery, not just the
     * owner and {@link #taggedPersons}. A shortcut for "tag everyone" without picking each person.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean visibleToFamily;
}

package com.example.lifecapsule.entity;

import com.example.lifecapsule.entity.enumirated.RelationshipType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "relationships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_relationship_family_people_type",
                        columnNames = {"family_id", "from_person_id", "to_person_id", "type"}
                )
        },
        indexes = {
                @Index(name = "idx_relationship_family", columnList = "family_id"),
                @Index(name = "idx_relationship_from_person", columnList = "from_person_id"),
                @Index(name = "idx_relationship_to_person", columnList = "to_person_id"),
                @Index(name = "idx_relationship_type", columnList = "type")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Relationship extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_person_id", nullable = false)
    private Person fromPerson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_person_id", nullable = false)
    private Person toPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RelationshipType type;

    @Column(columnDefinition = "TEXT")
    private String note;
}
